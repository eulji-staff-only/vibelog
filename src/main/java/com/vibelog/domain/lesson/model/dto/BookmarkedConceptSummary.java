package com.vibelog.domain.lesson.model.dto;

import java.time.LocalDateTime;

/* 북마크한 개념 한 줄 (LessonReader.findBookmarkedConcepts, 마이페이지 북마크 탭 auth API-USR-15 P2, FR-LSN-004) */
public record BookmarkedConceptSummary(Long courseId, Long lessonId, Long activityId, String title, String courseTitle,
                                       String lessonTitle, LocalDateTime bookmarkedAt) {
}
