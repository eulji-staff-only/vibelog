package com.vibelog.domain.lesson.model.dto;

/* 수업 비공개 영향 확인 (API-ADM-32, FR-LSN-014, #118). JSON 그대로 응답한다 (D-08) */
public record LessonImpactResult(int activeLearnerCount) {
}
