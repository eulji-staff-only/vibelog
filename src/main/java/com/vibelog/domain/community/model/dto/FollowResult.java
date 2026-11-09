package com.vibelog.domain.community.model.dto;

/* 팔로우·취소 결과 (API-CMT-15 JSON {"following", "followerCount"}) */
public record FollowResult(boolean following, long followerCount) {
}
