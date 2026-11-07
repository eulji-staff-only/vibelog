package com.vibelog.domain.lesson.model.dto;

/*
 * [CourseCompleteLessonReport]
 * 역할 : 코스 완료 화면(U-18, API-ENR-03)의 수업별 리포트 목록 한 줄.
 * 책임 : reportUrl 은 그 수업에 제출한 모의고사 응시가 있을 때만 값이 있다 (계약 ④ 회차로 만든 링크).
 *        제출한 응시가 없으면 null 이고, 화면은 "아직 응시하지 않았습니다" 같은 안내로 대신한다.
 */
public record CourseCompleteLessonReport(
        Long lessonId,
        String title,
        String reportUrl
) {
}
