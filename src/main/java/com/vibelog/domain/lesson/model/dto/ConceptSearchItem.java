package com.vibelog.domain.lesson.model.dto;

/* 개념 태그 검색 결과 한 줄 (API-LSN-09 JSON) */
public record ConceptSearchItem(Long activityId, String title, Long courseId, Long lessonId,
                                String courseTitle, String lessonTitle) {
}
