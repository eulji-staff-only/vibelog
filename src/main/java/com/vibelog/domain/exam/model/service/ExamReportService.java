package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.exam.model.dao.ExamAnswerRepository;
import com.vibelog.domain.exam.model.dao.ExamAttemptRepository;
import com.vibelog.domain.exam.model.dao.ExamChoiceRepository;
import com.vibelog.domain.exam.model.dto.ExamReportView;
import com.vibelog.domain.exam.model.entity.ExamAnswer;
import com.vibelog.domain.exam.model.entity.ExamAttempt;
import com.vibelog.domain.exam.model.entity.ExamChoice;
import com.vibelog.domain.exam.model.entity.ExamQuestion;
import com.vibelog.domain.lesson.model.policy.ExamLessonProgress;
import com.vibelog.domain.lesson.model.policy.ReportNextStep;
import com.vibelog.domain.lesson.model.service.LessonReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * [ExamReportService]
 * 역할 : 오답 분석 리포트 (API-EXM-05, FR-EXM-003·004).
 * 책임 : - 회차(attempt)를 고르지 않으면 최근 제출한 응시를 보여 준다. 남의 응시·제출 전 응시는 없는 것으로 본다
 *          (Controller 가 EXAM_NOT_SUBMITTED 로 모의고사 화면에 보낸다).
 *        - 처음 열면 report_viewed_at 을 남기고(PL-EXM-007), 같은 트랜잭션에서 lesson 계약 ③ recordReportViewed 로
 *          수업 완료 + 다음 수업(마지막이면 포트폴리오)을 연다. 다시 열면 연 일시를 다시 쓰지 않는다.
 *          계약 ③은 수업 완료를 처음 한 번만 기록하고 다음 행동만 돌려주므로, 다시 열 때도 다음 행동을 얻으려고 부른다.
 *        - 채점은 제출 때 끝나 있다 (exam_answer.is_correct). 리포트는 저장된 결과로만 그린다.
 *        - 개념 이름·수업 제목·코스 ID 는 lesson 영역 LessonReader 로 읽는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExamReportService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamChoiceRepository examChoiceRepository;
    private final ExamLessonProgress examLessonProgress;
    private final LessonReader lessonReader;

    public Optional<ExamReportView> getReport(Long memberId, Long lessonId, Integer attemptNo) {
        Optional<ExamAttempt> found = attemptNo == null
                ? examAttemptRepository.findFirstByMemberIdAndLessonIdAndSubmittedAtIsNotNullOrderByAttemptNoDesc(memberId, lessonId)
                : examAttemptRepository.findByMemberIdAndLessonIdAndAttemptNo(memberId, lessonId, attemptNo);
        if (found.isEmpty() || !found.get().isSubmitted()) {
            return Optional.empty();
        }
        ExamAttempt attempt = found.get();
        attempt.markReportViewed();
        ReportNextStep next = examLessonProgress.recordReportViewed(memberId, lessonId);

        List<ExamAnswer> answers = examAnswerRepository.findByExamAttempt(attempt).stream()
                .sorted(Comparator.comparingInt(answer -> answer.getExamQuestion().getSortOrder()))
                .toList();
        List<ExamQuestion> questions = answers.stream().map(ExamAnswer::getExamQuestion).toList();
        Map<Long, String> correctChoices = questions.isEmpty() ? Map.of()
                : examChoiceRepository.findByExamQuestionInOrderBySortOrderAsc(questions).stream()
                        .filter(ExamChoice::isCorrect)
                        .collect(Collectors.toMap(choice -> choice.getExamQuestion().getExamQuestionId(),
                                ExamChoice::getContent, (first, second) -> first));
        Map<Long, String> conceptTitles = lessonReader.getActivityTitles(
                questions.stream().map(ExamQuestion::getConceptActivityId).distinct().toList());

        return Optional.of(new ExamReportView(lessonId, next.courseId(),
                lessonReader.getLessonTitles(List.of(lessonId)).get(lessonId), attempt.getAttemptNo(),
                (int) answers.stream().filter(this::isCorrect).count(), answers.size(),
                concepts(answers, conceptTitles), wrongs(answers, correctChoices, conceptTitles), next.nextLessonId()));
    }

    /* 문항 순서대로 처음 나온 개념부터. 정답률 = 맞힌 수 ÷ 그 개념 문항 수 × 100 (정수로 반올림) */
    private List<ExamReportView.Concept> concepts(List<ExamAnswer> answers, Map<Long, String> conceptTitles) {
        Map<Long, List<ExamAnswer>> byConcept = answers.stream().collect(Collectors.groupingBy(
                answer -> answer.getExamQuestion().getConceptActivityId(), LinkedHashMap::new, Collectors.toList()));
        List<ExamReportView.Concept> concepts = new ArrayList<>();
        byConcept.forEach((activityId, group) -> {
            long correct = group.stream().filter(this::isCorrect).count();
            int rate = (int) Math.round(correct * 100.0 / group.size());
            concepts.add(new ExamReportView.Concept(activityId, conceptTitles.get(activityId), rate));
        });
        return concepts;
    }

    /* 틀린 문항(고르지 않은 문항 포함). 번호는 이 회차 문항 순서 */
    private List<ExamReportView.Wrong> wrongs(List<ExamAnswer> answers, Map<Long, String> correctChoices,
                                              Map<Long, String> conceptTitles) {
        List<ExamReportView.Wrong> wrongs = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            ExamAnswer answer = answers.get(i);
            if (isCorrect(answer)) {
                continue;
            }
            ExamQuestion question = answer.getExamQuestion();
            wrongs.add(new ExamReportView.Wrong(i + 1, question.getContent(),
                    Optional.ofNullable(answer.getExamChoice()).map(ExamChoice::getContent).orElse(null),
                    correctChoices.get(question.getExamQuestionId()), question.getExplanation(),
                    question.getConceptActivityId(), conceptTitles.get(question.getConceptActivityId())));
        }
        return wrongs;
    }

    private boolean isCorrect(ExamAnswer answer) {
        return Boolean.TRUE.equals(answer.getCorrect());
    }
}
