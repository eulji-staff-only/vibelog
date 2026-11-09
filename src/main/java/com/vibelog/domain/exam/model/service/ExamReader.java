package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.exam.model.dao.ExamAnswerRepository;
import com.vibelog.domain.exam.model.dao.ExamAttemptRepository;
import com.vibelog.domain.exam.model.dao.ExamChoiceRepository;
import com.vibelog.domain.exam.model.dao.ExamQuestionRepository;
import com.vibelog.domain.exam.model.dto.AdminExamQuestionItem;
import com.vibelog.domain.exam.model.dto.ExamReportView;
import com.vibelog.domain.exam.model.dto.ExamWeakConcept;
import com.vibelog.domain.exam.model.dto.MemberExamResult;
import com.vibelog.domain.exam.model.entity.ExamAnswer;
import com.vibelog.domain.exam.model.entity.ExamAttempt;
import com.vibelog.domain.exam.model.entity.ExamQuestion;
import com.vibelog.domain.exam.model.policy.ExamStatPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [ExamReader]
 * 역할 : 다른 영역이 exam 의 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용 메서드만 둔다. 목록용 메서드는 ID 묶음을 받는다 (예: Map<Long, X> getXxx(Collection<Long> ids)).
 *        - 필요한 메서드가 없으면 부르는 쪽이 추가한다. 기존 메서드의 이름·입력·출력은 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamReader {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamChoiceRepository examChoiceRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final MemberReader memberReader;

    /* 회원이 제출한 모의고사 결과, 최근 제출 순. 운영자 회원 상세(API-ADM-38, auth #119)가 쓴다. 제출 전 응시는 뺀다 */
    public List<MemberExamResult> getSubmittedResultsOf(Long memberId) {
        return examAttemptRepository.findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(memberId).stream()
                .map(attempt -> new MemberExamResult(attempt.getLessonId(), attempt.getAttemptNo(), attempt.getScore(),
                        attempt.getSubmittedAt()))
                .toList();
    }

    /*
     * 수업 ID → 기간 내(exam_attempt.submitted_at 기준) 제출한 1회차 응시의 평균 점수 (소수 첫째 자리).
     * 오답 분석 리포트(U-16)의 "같은 수업 응시자 평균"과 같은 기준이다 (PL-EXM-004·008, ExamStatPolicy):
     * 학습자마다 1회차 점수만, 운영자·테스트 계정 제외, 응시자가 5명 미만인 수업은 결과에 빠진다. from·to 가 null 이면 그 쪽 제한 없음.
     * 운영자 확장 대시보드(admin #194, FR-ADM-003)가 쓴다
     */
    public Map<Long, Double> getAverageScoreByLesson(LocalDateTime from, LocalDateTime to) {
        Map<Long, Double> averages = new HashMap<>();
        countedAttemptsByLesson(from, to).forEach((lessonId, attempts) ->
                averages.put(lessonId, ExamStatPolicy.average(attempts.stream().map(ExamAttempt::getScore).toList())));
        return averages;
    }

    /*
     * 수업 ID → 약한 개념(문항의 연관 개념 activity 별 정답률 60% 미만, PL-LSN-008) 목록, 정답률이 낮은 것부터.
     * 대상 응시는 getAverageScoreByLesson 과 같다(기간 내 1회차 제출, 운영자·테스트 계정 제외, 응시자 5명 미만 수업은 숨김).
     * 약한 개념이 없는 수업도 빈 목록으로 들어 있다. 답하지 않은 문항은 틀린 것으로 센다(오답 분석 리포트와 같다).
     * 운영자 확장 대시보드(admin #194, FR-ADM-003)가 쓴다
     */
    public Map<Long, List<ExamWeakConcept>> getWeakConceptsByLesson(LocalDateTime from, LocalDateTime to) {
        Map<Long, List<ExamAttempt>> attemptsByLesson = countedAttemptsByLesson(from, to);
        List<ExamAttempt> attempts = attemptsByLesson.values().stream().flatMap(List::stream).toList();
        if (attempts.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> lessonByAttempt = attempts.stream()
                .collect(Collectors.toMap(ExamAttempt::getExamAttemptId, ExamAttempt::getLessonId));
        // 수업 → 개념 activity → [맞힌 수, 답한 수]
        Map<Long, Map<Long, int[]>> tally = new HashMap<>();
        for (ExamAnswer answer : examAnswerRepository.findByExamAttemptIn(attempts)) {
            Long lessonId = lessonByAttempt.get(answer.getExamAttempt().getExamAttemptId());
            int[] counts = tally.computeIfAbsent(lessonId, id -> new HashMap<>())
                    .computeIfAbsent(answer.getExamQuestion().getConceptActivityId(), id -> new int[2]);
            counts[0] += Boolean.TRUE.equals(answer.getCorrect()) ? 1 : 0;
            counts[1]++;
        }
        Map<Long, List<ExamWeakConcept>> weak = new HashMap<>();
        for (Long lessonId : attemptsByLesson.keySet()) {
            weak.put(lessonId, tally.getOrDefault(lessonId, Map.of()).entrySet().stream()
                    .filter(entry -> entry.getValue()[0] * 100L < (long) ExamReportView.WEAK_RATE * entry.getValue()[1])
                    .map(entry -> new ExamWeakConcept(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                    .sorted(Comparator.comparingDouble((ExamWeakConcept concept) -> concept.correctCount() * 1.0 / concept.questionCount())
                            .thenComparing(ExamWeakConcept::conceptActivityId))
                    .toList());
        }
        return weak;
    }

    /* 통계에 넣는 응시: 기간 내 1회차 제출, 운영자·테스트 계정 제외, 수업별로 묶고 응시자가 5명 미만인 수업은 뺀다 (PL-EXM-008) */
    private Map<Long, List<ExamAttempt>> countedAttemptsByLesson(LocalDateTime from, LocalDateTime to) {
        List<ExamAttempt> firstAttempts = examAttemptRepository.findFirstAttemptsSubmittedBetween(from, to);
        Set<Long> excluded = memberReader.findStatExcludedIds(firstAttempts.stream().map(ExamAttempt::getMemberId).toList());
        Map<Long, List<ExamAttempt>> byLesson = firstAttempts.stream()
                .filter(attempt -> !excluded.contains(attempt.getMemberId()))
                .collect(Collectors.groupingBy(ExamAttempt::getLessonId));
        byLesson.values().removeIf(attempts -> !ExamStatPolicy.hasEnoughRespondents(attempts.size()));
        return byLesson;
    }

    /*
     * 수업의 모의고사 문항 목록, 순서대로. 운영자 수업 편집 화면(API-ADM-19, #100)이 쓴다.
     * 문항 추가·수정·사용 중지(API-ADM-29~31)는 이 메서드가 아니라 모의고사 문항 관리 이슈(FR-EXM-007)에서 만든다.
     */
    public List<AdminExamQuestionItem> getQuestionsOf(Long lessonId) {
        return examQuestionRepository.findByLessonIdOrderBySortOrderAsc(lessonId).stream()
                .map(question -> new AdminExamQuestionItem(question.getExamQuestionId(), question.getContent(),
                        question.getExplanation(), question.getConceptActivityId(), question.getSortOrder(),
                        question.isActive(), choicesOf(question)))
                .toList();
    }

    private List<AdminExamQuestionItem.Choice> choicesOf(ExamQuestion question) {
        return examChoiceRepository.findByExamQuestion_ExamQuestionIdOrderBySortOrderAsc(question.getExamQuestionId()).stream()
                .map(choice -> new AdminExamQuestionItem.Choice(choice.getContent(), choice.isCorrect(), choice.getSortOrder()))
                .toList();
    }
}
