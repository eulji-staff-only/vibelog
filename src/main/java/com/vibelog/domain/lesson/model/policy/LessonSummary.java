package com.vibelog.domain.lesson.model.policy;

/*
 * 코스 상세의 공개 수업 카드 (계약 ⑪, API-CRS-03).
 * lesson_id sort_order 순으로 돌려준다. 공개 수업은 모두 모의고사가 있다 (API-ADM-33)
 */
public record LessonSummary(Long lessonId, String title, String goal, int conceptCount, int quizCount,
                             int estimatedMinutes) {
}
