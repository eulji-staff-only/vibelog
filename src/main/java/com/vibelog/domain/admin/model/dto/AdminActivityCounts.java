package com.vibelog.domain.admin.model.dto;

/*
 * 확장 대시보드(API-ADM-40)의 활동 건수. 모두 조회 기간(from~to)에 생긴 건수다.
 * 2차(이탈률·퀴즈·모의고사 분석)는 이 record 가 아니라 AdminDashboardResponse 에 필드를 더해 붙인다.
 */
public record AdminActivityCounts(long portfolioCount, long postCount, long peerReviewCount, long messageCount,
                                  long reportCount, long contestEntryCount) {
}
