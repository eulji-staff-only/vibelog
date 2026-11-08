package com.vibelog.domain.lesson.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * [CourseCompleteView]
 * 역할 : 코스 완료 화면(U-18, API-ENR-03)의 응답값.
 * 책임 : completed·completedAt 은 계약 ⑦이 이미 기록한 enrollment.completed_at 을 그대로 읽은 값이다
 *        (완료 기록 로직은 이 화면이 다시 만들지 않는다, PL-ENR-003).
 */
public record CourseCompleteView(
        Long courseId,
        String trackName,
        String courseName,
        boolean completed,
        LocalDateTime completedAt,
        List<CourseCompleteCondition> conditions,
        List<CourseCompleteLessonReport> lessonReports
) {
}
