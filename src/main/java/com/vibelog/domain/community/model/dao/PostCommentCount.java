package com.vibelog.domain.community.model.dao;

/* 게시글 ID 별 댓글 수 (삭제된 댓글 제외). 목록에서 글마다 세지 않고 한 번에 센다 */
public record PostCommentCount(Long postId, long count) {
}
