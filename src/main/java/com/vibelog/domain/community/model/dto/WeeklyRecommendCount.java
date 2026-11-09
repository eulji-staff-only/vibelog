package com.vibelog.domain.community.model.dto;

import java.time.LocalDateTime;

/* 랭킹 집계 한 줄: 글, 이번 주 받은 추천 수, 이번 주 첫 추천 시각 (동점 정렬, PL-CMT-004) */
public record WeeklyRecommendCount(Long postId, long count, LocalDateTime firstRecommendedAt) {
}
