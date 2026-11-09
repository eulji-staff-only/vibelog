package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.lesson.model.policy.NextStep;
import com.vibelog.domain.lesson.model.policy.QuizActivityInfo;
import com.vibelog.domain.lesson.model.policy.QuizActivityProgress;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.quiz.exception.QuizErrorCode;
import com.vibelog.domain.quiz.model.dao.QuizAnswerRepository;
import com.vibelog.domain.quiz.model.dao.QuizAttemptRepository;
import com.vibelog.domain.quiz.model.dao.QuizChoiceRepository;
import com.vibelog.domain.quiz.model.dao.QuizQuestionRepository;
import com.vibelog.domain.quiz.model.dto.QuizCheckResponse;
import com.vibelog.domain.quiz.model.dto.QuizPageView;
import com.vibelog.domain.quiz.model.dto.QuizStartResponse;
import com.vibelog.domain.quiz.model.dto.QuizSubmitResponse;
import com.vibelog.domain.quiz.model.entity.QuizAnswer;
import com.vibelog.domain.quiz.model.entity.QuizAttempt;
import com.vibelog.domain.quiz.model.entity.QuizChoice;
import com.vibelog.domain.quiz.model.entity.QuizQuestion;
import com.vibelog.domain.quiz.model.policy.QuizGradingPolicy;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [QuizSolveService]
 * 역할 : 퀴즈 화면·풀이 시작·문항 확인·제출 채점 (API-LSN-03~06, FR-LSN-006).
 * 책임 : - 활동이 열려 있는지와 통과 기준은 lesson 영역 계약 ① getQuizActivity 로 묻는다. 닫혀 있으면 ACTIVITY_LOCKED.
 *        - 풀이 시작 응답에는 정답을 넣지 않는다. 다시 풀면 문항·보기 순서를 섞는다 (PL-LSN-004·005).
 *        - "확인"한 문항은 그 풀이 안에서 바꿀 수 없다 → ALREADY_CHECKED (PL-LSN-006). 남의 풀이는 NOT_OWNER.
 *        - 모든 문항을 확인해야 제출된다 → QUIZ_NOT_FINISHED. 채점 규칙은 QuizGradingPolicy (PL-LSN-002).
 *        - 통과하면 계약 ① recordQuizPassed 로 퀴즈 완료·다음 개념(마지막 퀴즈면 모의고사)을 연다.
 *        - 채점 결과에 개념 태그별 오답 비율과 약한 개념(정답률 60% 미만)을 함께 준다 (FR-LSN-007, PL-LSN-008, #109).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSolveService {

    static final String CONCEPT_OPENED = "다음 개념이 열렸습니다";
    static final String EXAM_OPENED = "모의고사가 열렸습니다";

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizGradingPolicy quizGradingPolicy;
    private final QuizActivityProgress quizActivityProgress;
    private final LessonReader lessonReader;

    /*
     * U-14 퀴즈 화면 틀 (API-LSN-03). 활동이 닫혀 있으면 빈 값 → Controller 가 수업 화면 + 토스트로 보낸다
     * (ACTIVITY_LOCKED 는 에러 화면이 아니라 "다른 화면 + 토스트"라서 예외로 던지지 않는다, _template.md 0-1 ③)
     */
    public Optional<QuizPageView> getPage(Long memberId, Long activityId) {
        QuizActivityInfo activity = quizActivityProgress.getQuizActivity(memberId, activityId);
        if (!activity.unlocked()) {
            return Optional.empty();
        }
        String title = lessonReader.getActivityTitles(List.of(activityId)).get(activityId);
        BigDecimal best = quizAttemptRepository
                .findTopByMemberIdAndActivityIdAndSubmittedAtIsNotNullOrderByCorrectRateDesc(memberId, activityId)
                .map(QuizAttempt::getCorrectRate)
                .orElse(null);
        return Optional.of(new QuizPageView(activity.courseId(), activity.lessonId(), activityId, title,
                quizQuestionRepository.countByActivityIdAndIsActiveTrue(activityId), best));
    }

    /* API-LSN-04 풀이 시작. 처음 풀면 정해진 순서, 다시 풀면 문항·보기 순서를 섞는다 */
    @Transactional
    public QuizStartResponse start(Long memberId, Long activityId) {
        openActivity(memberId, activityId);
        boolean retry = quizAttemptRepository.existsByMemberIdAndActivityId(memberId, activityId);
        QuizAttempt attempt = quizAttemptRepository.save(QuizAttempt.create(memberId, activityId, LocalDateTime.now()));

        List<QuizQuestion> questions = new ArrayList<>(quizQuestionRepository.findByActivityIdAndIsActiveTrueOrderBySortOrderAsc(activityId));
        Map<Long, List<QuizChoice>> choicesByQuestion = quizChoiceRepository
                .findByQuizQuestionIdInOrderBySortOrderAsc(questions.stream().map(QuizQuestion::getQuizQuestionId).toList())
                .stream()
                .collect(Collectors.groupingBy(QuizChoice::getQuizQuestionId));
        if (retry) {
            Collections.shuffle(questions);
        }

        List<QuizStartResponse.Question> body = questions.stream()
                .map(question -> {
                    List<QuizChoice> choices = new ArrayList<>(choicesByQuestion.getOrDefault(question.getQuizQuestionId(), List.of()));
                    if (retry) {
                        Collections.shuffle(choices);
                    }
                    return new QuizStartResponse.Question(question.getQuizQuestionId(), question.getDisplayType().name(),
                            question.getContent(), question.getCodeSnippet(),
                            choices.stream().map(choice -> new QuizStartResponse.Choice(choice.getQuizChoiceId(), choice.getContent())).toList());
                })
                .toList();
        return new QuizStartResponse(attempt.getQuizAttemptId(), body);
    }

    /* API-LSN-05 문항 확인: 답을 저장하고 정답·해설을 돌려준다. 이 뒤로 이 문항의 답은 바꿀 수 없다 */
    @Transactional
    public QuizCheckResponse check(Long memberId, Long quizAttemptId, Long quizQuestionId, Long quizChoiceId) {
        QuizAttempt attempt = ownOpenAttempt(memberId, quizAttemptId);

        QuizQuestion question = quizQuestionRepository.findById(quizQuestionId)
                .filter(q -> q.getActivityId().equals(attempt.getActivityId()) && q.isActive())
                .orElseThrow(() -> new BusinessException(QuizErrorCode.INVALID_INPUT));
        List<QuizChoice> choices = quizChoiceRepository.findByQuizQuestionId(quizQuestionId);
        QuizChoice picked = choices.stream()
                .filter(choice -> choice.getQuizChoiceId().equals(quizChoiceId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(QuizErrorCode.INVALID_INPUT));
        if (quizAnswerRepository.existsByQuizAttemptIdAndQuizQuestionId(quizAttemptId, quizQuestionId)) {
            throw new BusinessException(QuizErrorCode.ALREADY_CHECKED);
        }

        quizAnswerRepository.save(QuizAnswer.create(quizAttemptId, quizQuestionId, quizChoiceId, picked.isCorrect(), LocalDateTime.now()));
        Long correctChoiceId = choices.stream()
                .filter(QuizChoice::isCorrect)
                .map(QuizChoice::getQuizChoiceId)
                .findFirst()
                .orElse(null);
        return new QuizCheckResponse(picked.isCorrect(), correctChoiceId, question.getExplanation());
    }

    /* API-LSN-06 제출·채점. 모든 문항을 확인해야 한다. 통과하면 계약 ① 로 다음 개념(또는 모의고사)을 연다 */
    @Transactional
    public QuizSubmitResponse submit(Long memberId, Long quizAttemptId) {
        QuizAttempt attempt = ownOpenAttempt(memberId, quizAttemptId);
        Long activityId = attempt.getActivityId();

        List<QuizQuestion> questions = quizQuestionRepository.findByActivityIdAndIsActiveTrueOrderBySortOrderAsc(activityId);
        Set<Long> questionIds = questions.stream()
                .map(QuizQuestion::getQuizQuestionId)
                .collect(Collectors.toSet());
        Map<Long, QuizAnswer> answerByQuestion = quizAnswerRepository.findByQuizAttemptId(quizAttemptId).stream()
                .filter(answer -> questionIds.contains(answer.getQuizQuestionId()))
                .collect(Collectors.toMap(QuizAnswer::getQuizQuestionId, answer -> answer));
        if (questionIds.isEmpty() || answerByQuestion.size() < questionIds.size()) {
            throw new BusinessException(QuizErrorCode.QUIZ_NOT_FINISHED);
        }

        QuizActivityInfo activity = quizActivityProgress.getQuizActivity(memberId, activityId);
        int correctCount = (int) answerByQuestion.values().stream().filter(QuizAnswer::isCorrect).count();
        BigDecimal correctRate = quizGradingPolicy.correctRate(correctCount, questionIds.size());
        boolean passed = quizGradingPolicy.isPassed(correctRate, activity.passRate());
        attempt.submit(correctRate, passed, LocalDateTime.now());
        List<QuizSubmitResponse.TagStat> tagStats = tagStatsOf(questions, answerByQuestion);

        String lessonUrl = "/courses/" + activity.courseId() + "/lessons/" + activity.lessonId();
        if (!passed) {
            return new QuizSubmitResponse(correctRate, false, tagStats, new QuizSubmitResponse.Next(NextStep.NONE, lessonUrl), null);
        }
        NextStep next = quizActivityProgress.recordQuizPassed(memberId, activityId);
        return switch (next.type()) {
            case NextStep.CONCEPT -> new QuizSubmitResponse(correctRate, true, tagStats,
                    new QuizSubmitResponse.Next(NextStep.CONCEPT, lessonUrl + "?activity=" + next.activityId()), CONCEPT_OPENED);
            case NextStep.EXAM -> new QuizSubmitResponse(correctRate, true, tagStats,
                    new QuizSubmitResponse.Next(NextStep.EXAM, lessonUrl), EXAM_OPENED);
            default -> new QuizSubmitResponse(correctRate, true, tagStats,
                    new QuizSubmitResponse.Next(NextStep.NONE, lessonUrl), null);
        };
    }

    /*
     * 개념 태그별 오답 비율과 약한 개념 (FR-LSN-007, PL-LSN-008). 문항 순서대로 처음 나온 태그 순서로 보여준다.
     * 태그 이름은 lesson 영역의 LessonReader 로 읽는다 (concept_tag 는 lesson 영역 테이블).
     */
    private List<QuizSubmitResponse.TagStat> tagStatsOf(List<QuizQuestion> questions, Map<Long, QuizAnswer> answerByQuestion) {
        Map<Long, List<QuizQuestion>> questionsByTag = questions.stream()
                .collect(Collectors.groupingBy(QuizQuestion::getConceptTagId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, String> tagNames = lessonReader.getConceptTagNames(questionsByTag.keySet());

        return questionsByTag.entrySet().stream()
                .map(entry -> {
                    int total = entry.getValue().size();
                    int correct = (int) entry.getValue().stream()
                            .filter(question -> answerByQuestion.get(question.getQuizQuestionId()).isCorrect())
                            .count();
                    return new QuizSubmitResponse.TagStat(tagNames.get(entry.getKey()),
                            quizGradingPolicy.wrongRate(total - correct, total), quizGradingPolicy.isWeak(correct, total));
                })
                .toList();
    }

    private QuizActivityInfo openActivity(Long memberId, Long activityId) {
        QuizActivityInfo activity = quizActivityProgress.getQuizActivity(memberId, activityId);
        if (!activity.unlocked()) {
            throw new BusinessException(QuizErrorCode.ACTIVITY_LOCKED);
        }
        return activity;
    }

    /* 본인 풀이이고 아직 제출하지 않았어야 한다. 없는 풀이도 남의 것처럼 NOT_OWNER (있는지 알리지 않는다) */
    private QuizAttempt ownOpenAttempt(Long memberId, Long quizAttemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(quizAttemptId)
                .filter(found -> found.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_OWNER));
        if (attempt.isSubmitted()) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_SUBMITTED);
        }
        return attempt;
    }
}
