package com.vibelog.domain.contest.model.dto;

/* 배지 한 개 (ContestReader.findBadgesOf): 마이페이지 배지 탭·공개 프로필이 쓴다 (FR-RWD-002) */
public record BadgeView(Long contestAwardId, String name, String contestMonth, int rank) {
}
