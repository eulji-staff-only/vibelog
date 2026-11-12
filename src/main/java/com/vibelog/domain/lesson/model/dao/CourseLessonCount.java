package com.vibelog.domain.lesson.model.dao;

/* 코스별 공개 수업 수 집계 결과 (LessonRepository.countPublicLessonsByCourseIds 전용) */
public record CourseLessonCount(Long courseId, long count) {
}
