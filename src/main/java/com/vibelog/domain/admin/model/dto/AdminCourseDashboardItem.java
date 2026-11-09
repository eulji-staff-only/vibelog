package com.vibelog.domain.admin.model.dto;

/*
 * 대시보드(A-01, API-ADM-01)의 코스 한 줄. enrollmentCount 는 조회 기간에 시작한 수강 수,
 * completionRate 는 그중 완료한 수강 비율(%, 정수)이다 (FR-ADM-002).
 */
public record AdminCourseDashboardItem(Long courseId, String courseTitle, long enrollmentCount, int completionRate) {
}
