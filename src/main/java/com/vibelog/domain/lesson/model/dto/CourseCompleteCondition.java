package com.vibelog.domain.lesson.model.dto;

/*
 * [CourseCompleteCondition]
 * 역할 : 코스 완료 화면(U-18, API-ENR-03)의 완료 조건 한 줄(PL-ENR-003).
 */
public record CourseCompleteCondition(
        String name,
        boolean met
) {
}
