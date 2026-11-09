package com.vibelog.domain.community.model.dto;

/* 회원 한 명 (마이페이지 팔로우 탭). auth 영역이 CommunityReader 로 받아 간다 */
public record MemberSummary(Long memberId, String nickname) {
}
