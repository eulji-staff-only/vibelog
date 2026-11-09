package com.vibelog.domain.admin.model.dto;

import java.util.List;

/*
 * 대시보드(A-01, API-ADM-01)의 트랙 한 묶음. enrollmentCount·completionRate 는 소속 코스 집계를 합친 값이다
 * (FR-ADM-002). courses 는 코스 이름 순으로 둔다.
 */
public record AdminTrackDashboardItem(Long trackId, String trackName, long enrollmentCount, int completionRate,
                                       List<AdminCourseDashboardItem> courses) {
}
