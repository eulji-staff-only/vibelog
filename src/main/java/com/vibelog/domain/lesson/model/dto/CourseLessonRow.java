package com.vibelog.domain.lesson.model.dto;

/*
 * 운영자 코스 상세(A-05, course #96)의 수업 목록 한 줄 (LessonReader.getLessonsOfCourse).
 * 비공개 수업도 포함한다. sort_order 순으로 돌려준다.
 */
public record CourseLessonRow(Long lessonId, String title, int sortOrder, boolean isPublic) {
}
