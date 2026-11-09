package com.vibelog.domain.admin.model.dto;

import java.util.List;

/*
 * 대시보드(A-01, API-ADM-01·40) 응답. memberCount 는 조회 기간에 가입한 회원 수(FR-ADM-002),
 * tracks 는 트랙 이름 순으로 둔다. 기간에 시작한 수강이 없으면 tracks 는 빈 목록이다.
 * diagnosis·activity 는 확장 대시보드(FR-ADM-003) 1차 지표다. 2차 지표는 이 record 에 필드를 더해 붙인다.
 */
public record AdminDashboardResponse(long memberCount, List<AdminTrackDashboardItem> tracks,
                                     AdminDiagnosisStat diagnosis, AdminActivityCounts activity) {
}
