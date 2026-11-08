package com.vibelog.domain.community.model.dto;

import java.time.LocalDateTime;

/* 마이페이지 "작성한 댓글" 한 줄 (API-USR-15). 누르면 그 글의 댓글 위치(#comment-{id})로 간다 */
public record MemberCommentItem(Long postId, Long commentId, String content, String postTitle, LocalDateTime createdAt) {
}
