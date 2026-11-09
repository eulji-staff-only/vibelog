package com.vibelog.domain.lesson.model.dto;

/* 연관 개념 한 줄 (API-LSN-07). 누르면 /courses/{courseId}/lessons/{lessonId}?activity={relatedActivityId} */
public record RelatedConcept(Long relatedActivityId, String title, Long courseId, Long lessonId) {
}
