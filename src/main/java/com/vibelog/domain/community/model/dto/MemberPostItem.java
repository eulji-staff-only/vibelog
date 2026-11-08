package com.vibelog.domain.community.model.dto;

import java.time.LocalDateTime;

/* 마이페이지 "작성한 글" 한 줄 (API-USR-15). auth 영역이 CommunityReader 로 받아 간다 */
public record MemberPostItem(Long postId, String title, String categoryLabel, LocalDateTime createdAt, long commentCount) {
}
