package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.lesson.model.dto.QuizActivityPlace;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.quiz.model.dao.QuizAnswerRepository;
import com.vibelog.domain.quiz.model.dao.QuizAttemptRepository;
import com.vibelog.domain.quiz.model.dao.QuizChoiceRepository;
import com.vibelog.domain.quiz.model.dao.QuizQuestionRepository;
import com.vibelog.domain.quiz.model.dto.WrongAnswerView;
import com.vibelog.domain.quiz.model.entity.QuizAnswer;
import com.vibelog.domain.quiz.model.entity.QuizAttempt;
import com.vibelog.domain.quiz.model.entity.QuizChoice;
import com.vibelog.domain.quiz.model.entity.QuizQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [QuizWrongAnswerService]
 * 역할 : 틀린 문제 모아보기 (U-28, API-LSN-10, FR-LSN-008, #190).
 * 책임 : - 회원이 "확인"한 답(quiz_answer) 중 틀린 것을 모은다. 답은 확인하면 고정되므로 제출 전 풀이의 답도 모은다 (PL-LSN-006).
 *        - 같은 문항을 여러 번 틀렸으면 가장 최근 답 1개만. 나중에 맞혀도 빼지 않는다 (quiz.md 결정 1·2).
 *        - 운영자가 고치거나 중지한 문항도 그대로 보여준다 (내 답은 예전 문항·보기를 가리킨다, DB 설계서 5-4).
 *        - 코스·수업 위치와 개념 링크는 lesson 의 LessonReader, 코스 제목은 course 의 CourseReader 로 받는다.
 *          개념 링크는 문항의 개념 태그가 붙은 같은 수업의 사용 중인 개념 (quiz.md 결정 4). 없으면 수업 화면.
 *        - 없는 course·lesson 으로 거르면 빈 목록 (오류 아님).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizWrongAnswerService {

    /* 한 쪽 개수 (quiz.md 결정 3, 게시판 목록과 같은 수) */
    static final int PAGE_SIZE = 20;

    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final LessonReader lessonReader;
    private final CourseReader courseReader;

    public WrongAnswerView getPage(Long memberId, Long courseId, Long lessonId, int page) {
        // 1. 틀린 답, 문항마다 가장 최근 것 하나 (쿼리가 최근 순이라 먼저 나온 것을 남긴다)
        Map<Long, QuizAnswer> latestByQuestion = new LinkedHashMap<>();
        for (QuizAnswer answer : quizAnswerRepository.findWrongAnswersOf(memberId)) {
            latestByQuestion.putIfAbsent(answer.getQuizQuestionId(), answer);
        }
        List<QuizAnswer> answers = List.copyOf(latestByQuestion.values());

        // 2. 답 → 퀴즈 활동 → 코스·수업
        Map<Long, Long> activityByAttempt = quizAttemptRepository.findAllById(
                        answers.stream().map(QuizAnswer::getQuizAttemptId).distinct().toList()).stream()
                .collect(Collectors.toMap(QuizAttempt::getQuizAttemptId, QuizAttempt::getActivityId));
        Map<Long, QuizActivityPlace> places = lessonReader.getQuizActivityPlaces(Set.copyOf(activityByAttempt.values()));
        Function<QuizAnswer, QuizActivityPlace> placeOf = answer -> places.get(activityByAttempt.get(answer.getQuizAttemptId()));
        List<QuizAnswer> placed = answers.stream().filter(answer -> placeOf.apply(answer) != null).toList();

        // 3. 필터 칸: 내가 틀린 문항이 있는 코스·수업
        Set<Long> courseIds = placed.stream().map(answer -> placeOf.apply(answer).courseId()).collect(Collectors.toSet());
        Set<Long> lessonIds = placed.stream().map(answer -> placeOf.apply(answer).lessonId()).collect(Collectors.toSet());
        Map<Long, String> courseTitles = courseIds.isEmpty() ? Map.of() : courseReader.getCourseTitles(courseIds);
        Map<Long, String> lessonTitles = lessonIds.isEmpty() ? Map.of() : lessonReader.getLessonTitles(lessonIds);
        Map<Long, Long> courseByLesson = placed.stream().map(placeOf)
                .collect(Collectors.toMap(QuizActivityPlace::lessonId, QuizActivityPlace::courseId, (a, b) -> a));
        List<WrongAnswerView.Option> courseOptions = courseIds.stream()
                .map(id -> new WrongAnswerView.Option(id, courseTitles.getOrDefault(id, "코스"), id))
                .sorted(Comparator.comparing(WrongAnswerView.Option::id)).toList();
        List<WrongAnswerView.Option> lessonOptions = lessonIds.stream()
                .filter(id -> courseId == null || courseId.equals(courseByLesson.get(id)))
                .map(id -> new WrongAnswerView.Option(id, lessonTitles.getOrDefault(id, "수업"), courseByLesson.get(id)))
                .sorted(Comparator.comparing(WrongAnswerView.Option::id)).toList();

        // 4. 거르고 한 쪽 자르기
        List<QuizAnswer> filtered = placed.stream()
                .filter(answer -> courseId == null || courseId.equals(placeOf.apply(answer).courseId()))
                .filter(answer -> lessonId == null || lessonId.equals(placeOf.apply(answer).lessonId()))
                .toList();
        int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.min(Math.max(page, 1), totalPages);
        List<QuizAnswer> pageAnswers = filtered.stream().skip((long) (current - 1) * PAGE_SIZE).limit(PAGE_SIZE).toList();

        return new WrongAnswerView(items(pageAnswers, placeOf, courseTitles, lessonTitles), current, totalPages, filtered.size(),
                courseOptions, lessonOptions, courseId, lessonId);
    }

    private List<WrongAnswerView.Item> items(List<QuizAnswer> answers, Function<QuizAnswer, QuizActivityPlace> placeOf,
                                             Map<Long, String> courseTitles, Map<Long, String> lessonTitles) {
        if (answers.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = answers.stream().map(QuizAnswer::getQuizQuestionId).toList();
        Map<Long, QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(QuizQuestion::getQuizQuestionId, Function.identity()));
        Map<Long, List<QuizChoice>> choicesByQuestion = quizChoiceRepository.findByQuizQuestionIdInOrderBySortOrderAsc(questionIds).stream()
                .collect(Collectors.groupingBy(QuizChoice::getQuizQuestionId));
        Map<Long, String> tagNames = lessonReader.getConceptTagNames(questions.values().stream()
                .map(QuizQuestion::getConceptTagId).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new)));
        Map<Long, Map<Long, Long>> conceptByTag = lessonReader.getConceptActivityIdsByTag(
                answers.stream().map(answer -> placeOf.apply(answer).lessonId()).distinct().toList());

        return answers.stream()
                .filter(answer -> questions.containsKey(answer.getQuizQuestionId()))
                .map(answer -> {
                    QuizQuestion question = questions.get(answer.getQuizQuestionId());
                    QuizActivityPlace place = placeOf.apply(answer);
                    List<QuizChoice> choices = choicesByQuestion.getOrDefault(question.getQuizQuestionId(), List.of());
                    String myAnswer = choices.stream().filter(choice -> choice.getQuizChoiceId().equals(answer.getQuizChoiceId()))
                            .map(QuizChoice::getContent).findFirst().orElse("");
                    String correct = choices.stream().filter(QuizChoice::isCorrect).map(QuizChoice::getContent).findFirst().orElse("");
                    Long conceptActivityId = conceptByTag.getOrDefault(place.lessonId(), Map.of()).get(question.getConceptTagId());
                    String lessonUrl = "/courses/" + place.courseId() + "/lessons/" + place.lessonId();
                    return new WrongAnswerView.Item(question.getQuizQuestionId(), courseTitles.get(place.courseId()),
                            lessonTitles.get(place.lessonId()), question.getDisplayType().getLabel(), question.getContent(),
                            question.getCodeSnippet(), myAnswer, correct, question.getExplanation(),
                            tagNames.get(question.getConceptTagId()),
                            conceptActivityId == null ? lessonUrl : lessonUrl + "?activity=" + conceptActivityId);
                })
                .toList();
    }
}
