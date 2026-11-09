package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.dao.ExamAnswerRepository;
import com.vibelog.domain.exam.model.dao.ExamAttemptRepository;
import com.vibelog.domain.exam.model.dao.ExamChoiceRepository;
import com.vibelog.domain.exam.model.dao.ExamQuestionRepository;
import com.vibelog.domain.exam.model.dto.ExamPageView;
import com.vibelog.domain.exam.model.dto.ExamStartResult;
import com.vibelog.domain.exam.model.dto.ExamSubmitResult;
import com.vibelog.domain.exam.model.entity.ExamAnswer;
import com.vibelog.domain.exam.model.entity.ExamAttempt;
import com.vibelog.domain.exam.model.entity.ExamChoice;
import com.vibelog.domain.exam.model.entity.ExamQuestion;
import com.vibelog.domain.lesson.model.policy.ExamLessonProgress;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [ExamService]
 * 역할 : 모의고사 응시 시작·이어 풀기(API-EXM-01), 화면(API-EXM-02), 답 저장(API-EXM-03), 제출·채점(API-EXM-04).
 * 책임 : - 모의고사가 열렸는지는 lesson 영역의 계약 ②(ExamLessonProgress.isExamOpen)로 묻는다.
 *        - 수업 제목·코스 ID 는 LessonReader 로 받는다 (다른 영역 Entity 를 쓰지 않는다).
 *        - 제한 시간·통과 점수는 없다 (PL-EXM-005, PL-EXM-001). 채점은 규칙 기반이다 (PL-CST-001).
 *        - 수강 포기한 수강은 답 저장·제출하지 않는다 (EXAM_LOCKED, #306).
 *        - 회원 ID 는 Controller 가 로그인 회원에서 꺼내 넘긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExamService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamChoiceRepository examChoiceRepository;
    private final ExamLessonProgress examLessonProgress;
    private final LessonReader lessonReader;

    /*
     * API-EXM-01. 제출 안 한 응시가 있으면 그대로 이어 풀고, 없으면 새 회차를 만든다.
     * 직전 회차의 리포트를 보기 전에는 새로 응시할 수 없다 (PL-EXM-003).
     */
    public ExamStartResult start(Long memberId, Long lessonId) {
        if (!examLessonProgress.isExamOpen(memberId, lessonId)) {
            return ExamStartResult.locked();
        }
        Optional<ExamAttempt> latest = examAttemptRepository.findFirstByMemberIdAndLessonIdOrderByAttemptNoDesc(memberId, lessonId);
        if (latest.isPresent() && !latest.get().isSubmitted()) {
            return ExamStartResult.ok();
        }
        if (latest.isPresent() && !latest.get().isReportViewed()) {
            return ExamStartResult.reportNotViewed(latest.get().getAttemptNo());
        }
        int nextNo = latest.map(attempt -> attempt.getAttemptNo() + 1).orElse(1);
        examAttemptRepository.save(ExamAttempt.create(memberId, lessonId, nextNo));
        return ExamStartResult.ok();
    }

    /* API-EXM-02. 진행 중인 응시가 없으면 비어 있다 (Controller 가 수업 화면으로 보낸다) */
    @Transactional(readOnly = true)
    public Optional<ExamPageView> getPage(Long memberId, Long lessonId) {
        return examAttemptRepository.findFirstByMemberIdAndLessonIdOrderByAttemptNoDesc(memberId, lessonId)
                .filter(attempt -> !attempt.isSubmitted())
                .map(attempt -> pageOf(attempt, lessonId));
    }

    /* 수업 화면 주소에 쓸 코스 ID. 없는 수업이면 LESSON_NOT_FOUND */
    @Transactional(readOnly = true)
    public Long getCourseId(Long lessonId) {
        return lessonReader.getCourseId(lessonId);
    }

    /* API-EXM-03. 응시 중 고른 답을 바로 저장한다. 보기를 비우면 선택 취소 (PL-EXM-006) */
    public void saveAnswer(Long memberId, Long examAttemptId, Long examQuestionId, Long examChoiceId) {
        ExamAttempt attempt = getOwnAttempt(memberId, examAttemptId);
        if (attempt.isSubmitted()) {
            throw new BusinessException(ExamErrorCode.ATTEMPT_SUBMITTED);
        }
        requireExamOpen(memberId, attempt.getLessonId());
        Map<Long, ExamQuestion> questions = questionsOf(attempt.getLessonId());
        Map<Long, ExamAnswer> answers = answersOf(attempt);
        choose(attempt, questions, answers, examQuestionId, examChoiceId);
    }

    /*
     * API-EXM-04. 폼으로 온 마지막 답을 저장한 뒤 채점한다.
     * 사용 중인 문항 전체가 대상이고, 고르지 않은 문항은 오답이다 (M-02 안내).
     * 응시 중에 운영자가 사용 중지한 문항(API-ADM-31)은 채점 대상이 아니다. 폼에 남은 답은 건너뛰고,
     * 그 전에 저장해 둔 답은 지워 제출한 응시에 채점되지 않은 답이 남지 않게 한다.
     * score = 맞힌 문항 수, correct_rate = 맞힌 수 ÷ 문항 수 × 100 (소수 둘째 자리 반올림).
     */
    public ExamSubmitResult submit(Long memberId, Long lessonId, Long examAttemptId, Map<Long, Long> lastAnswers) {
        ExamAttempt attempt = getOwnAttempt(memberId, examAttemptId);
        if (!attempt.getLessonId().equals(lessonId)) {
            throw new BusinessException(ExamErrorCode.INVALID_INPUT);
        }
        if (attempt.isSubmitted()) {
            return new ExamSubmitResult(attempt.getAttemptNo(), true);
        }
        requireExamOpen(memberId, lessonId);
        Map<Long, ExamQuestion> questions = questionsOf(lessonId);
        Map<Long, ExamAnswer> answers = answersOf(attempt);
        List<ExamAnswer> stale = answers.values().stream()
                .filter(answer -> !questions.containsKey(answer.getExamQuestion().getExamQuestionId()))
                .toList();
        examAnswerRepository.deleteAll(stale);
        stale.forEach(answer -> answers.remove(answer.getExamQuestion().getExamQuestionId()));
        lastAnswers.entrySet().stream()
                .filter(entry -> questions.containsKey(entry.getKey()))
                .forEach(entry -> choose(attempt, questions, answers, entry.getKey(), entry.getValue()));

        int correctCount = 0;
        for (ExamQuestion question : questions.values()) {
            ExamAnswer answer = answers.computeIfAbsent(question.getExamQuestionId(),
                    id -> examAnswerRepository.save(ExamAnswer.create(attempt, question, null)));
            if (answer.grade()) {
                correctCount++;
            }
        }
        attempt.submit(correctCount, correctRate(correctCount, questions.size()));
        return new ExamSubmitResult(attempt.getAttemptNo(), false);
    }

    static BigDecimal correctRate(int correctCount, int total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(correctCount * 100L)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /* 수강을 포기해 모의고사가 닫혔으면 저장·제출하지 않는다 (#306) */
    private void requireExamOpen(Long memberId, Long lessonId) {
        if (!examLessonProgress.isExamOpen(memberId, lessonId)) {
            throw new BusinessException(ExamErrorCode.EXAM_LOCKED);
        }
    }

    /* 없는 응시도 남의 응시와 같이 막는다 (있는지 없는지 알려 주지 않는다) */
    private ExamAttempt getOwnAttempt(Long memberId, Long examAttemptId) {
        return examAttemptRepository.findById(examAttemptId)
                .filter(attempt -> attempt.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_OWNER));
    }

    /* 그 수업의 사용 중인 문항이 아니거나, 그 문항의 보기가 아니면 INVALID_INPUT */
    private void choose(ExamAttempt attempt, Map<Long, ExamQuestion> questions, Map<Long, ExamAnswer> answers,
                        Long examQuestionId, Long examChoiceId) {
        ExamQuestion question = questions.get(examQuestionId);
        if (question == null) {
            throw new BusinessException(ExamErrorCode.INVALID_INPUT);
        }
        ExamChoice choice = examChoiceId == null ? null : examChoiceRepository.findById(examChoiceId)
                .filter(found -> found.getExamQuestion().getExamQuestionId().equals(examQuestionId))
                .orElseThrow(() -> new BusinessException(ExamErrorCode.INVALID_INPUT));
        ExamAnswer answer = answers.get(examQuestionId);
        if (answer == null) {
            answers.put(examQuestionId, examAnswerRepository.save(ExamAnswer.create(attempt, question, choice)));
        } else {
            answer.choose(choice);
        }
    }

    private Map<Long, ExamQuestion> questionsOf(Long lessonId) {
        return examQuestionRepository.findByLessonIdAndActiveTrueOrderBySortOrderAsc(lessonId).stream()
                .collect(Collectors.toMap(ExamQuestion::getExamQuestionId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, ExamAnswer> answersOf(ExamAttempt attempt) {
        return examAnswerRepository.findByExamAttempt(attempt).stream()
                .collect(Collectors.toMap(answer -> answer.getExamQuestion().getExamQuestionId(), Function.identity()));
    }

    private ExamPageView pageOf(ExamAttempt attempt, Long lessonId) {
        List<ExamQuestion> questions = examQuestionRepository.findByLessonIdAndActiveTrueOrderBySortOrderAsc(lessonId);
        Map<Long, List<ExamPageView.Choice>> choices = questions.isEmpty() ? Map.of()
                : examChoiceRepository.findByExamQuestionInOrderBySortOrderAsc(questions).stream()
                .collect(Collectors.groupingBy(choice -> choice.getExamQuestion().getExamQuestionId(),
                        Collectors.mapping(choice -> new ExamPageView.Choice(choice.getExamChoiceId(), choice.getContent()),
                                Collectors.toList())));
        Map<Long, Long> selected = examAnswerRepository.findByExamAttempt(attempt).stream()
                .filter(answer -> answer.getExamChoice() != null)
                .collect(Collectors.toMap(answer -> answer.getExamQuestion().getExamQuestionId(),
                        answer -> answer.getExamChoice().getExamChoiceId()));

        List<ExamPageView.Question> items = questions.stream()
                .map(question -> new ExamPageView.Question(question.getExamQuestionId(), question.getContent(),
                        choices.getOrDefault(question.getExamQuestionId(), List.of()),
                        selected.get(question.getExamQuestionId())))
                .toList();
        String title = lessonReader.getLessonTitles(List.of(lessonId)).get(lessonId);
        return new ExamPageView(lessonId, lessonReader.getCourseId(lessonId), title,
                attempt.getExamAttemptId(), attempt.getAttemptNo(), items);
    }
}
