package com.vibelog.domain.admin.model.dto;

import java.util.List;

/*
 * 기본 대시보드(A-01, API-ADM-01) 응답. memberCount 는 조회 기간에 가입한 회원 수(FR-ADM-002),
 * tracks 는 트랙 이름 순으로 둔다. 기간에 시작한 수강이 없으면 tracks 는 빈 목록이다.
 */
public record AdminDashboardResponse(long memberCount, List<AdminTrackDashboardItem> tracks) {
}
