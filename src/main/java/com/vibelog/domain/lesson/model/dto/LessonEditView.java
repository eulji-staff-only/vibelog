package com.vibelog.domain.lesson.model.dto;

import com.vibelog.domain.exam.model.dto.AdminExamQuestionItem;

import java.util.List;

/*
 * [LessonEditView]
 * 역할 : 수업 편집 화면(A-06, API-ADM-19, #100)에 필요한 읽기 전용 데이터를 한 번에 담는다.
 * 책임 : 입력 폼 값은 이 안에 두지 않는다 (LessonAdminService.getForm, LessonFormRequest 가 따로 맡는다).
 */
public record LessonEditView(Long lessonId, Long courseId, String courseTitle, String title, boolean publicLesson,
                              String tab, List<AdminActivityItem> activities, List<ConceptTagOption> conceptTags,
                              List<AdminExamQuestionItem> examQuestions, LessonPublishStatus publishStatus) {
}
