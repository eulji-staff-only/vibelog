package com.vibelog.domain.lesson.model.dto;

import com.vibelog.domain.exam.model.dto.AdminExamQuestionItem;
import com.vibelog.domain.quiz.model.dto.AdminQuizQuestionItem;

import java.util.List;

/*
 * [LessonEditView]
 * 역할 : 수업 편집 화면(A-06, API-ADM-19, #100)에 필요한 읽기 전용 데이터를 한 번에 담는다.
 * 책임 : 입력 폼 값은 이 안에 두지 않는다 (LessonAdminService.getForm, LessonFormRequest 가 따로 맡는다).
 *        퀴즈 문항(quizQuestions)은 quiz 의 QuizReader 가 채우고, 활동 탭의 quiz/admin-quiz-questions 조각이 쓴다 (#102).
 */
public record LessonEditView(Long lessonId, Long courseId, String courseTitle, String title, boolean publicLesson,
                              String tab, List<AdminActivityItem> activities, List<ConceptTagOption> conceptTags,
                              List<AdminExamQuestionItem> examQuestions, LessonPublishStatus publishStatus,
                              List<AdminQuizQuestionItem> quizQuestions) {

    /* 퀴즈 문항 없이 만드는 기존 호출(테스트 등)을 그대로 두려고 남긴 생성자. 퀴즈 문항은 빈 목록 */
    public LessonEditView(Long lessonId, Long courseId, String courseTitle, String title, boolean publicLesson,
                          String tab, List<AdminActivityItem> activities, List<ConceptTagOption> conceptTags,
                          List<AdminExamQuestionItem> examQuestions, LessonPublishStatus publishStatus) {
        this(lessonId, courseId, courseTitle, title, publicLesson, tab, activities, conceptTags, examQuestions,
                publishStatus, List.of());
    }

    /* 한 퀴즈 활동의 문항들, 순서대로 (조각이 활동마다 묶어 보여 준다) */
    public List<AdminQuizQuestionItem> quizQuestionsOf(Long activityId) {
        return quizQuestions.stream().filter(question -> question.activityId().equals(activityId)).toList();
    }
}
