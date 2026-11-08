package com.vibelog.domain.community.model.dto;

import com.vibelog.domain.community.model.entity.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * 게시글 상세의 댓글 하나와 그 답글 (API-CMT-02).
 * 삭제된 댓글은 내용·작성자를 보여 주지 않는다 ("삭제된 댓글입니다", PL-CMT-006).
 */
public record CommentView(Long commentId, String nickname, String content, LocalDateTime createdAt,
                          boolean edited, boolean deleted, List<CommentView> replies) {

    /* 작성 순서대로 받은 댓글을 "댓글 → 그 아래 답글" 모양으로 묶는다. 답글은 한 단계까지만 있다 */
    public static List<CommentView> tree(List<Comment> comments, Map<Long, String> nicknames) {
        Map<Long, CommentView> topLevel = new LinkedHashMap<>();
        for (Comment comment : comments) {
            if (comment.getParentCommentId() == null) {
                topLevel.put(comment.getCommentId(), of(comment, nicknames, new ArrayList<>()));
            }
        }
        for (Comment comment : comments) {
            CommentView parent = comment.getParentCommentId() == null ? null : topLevel.get(comment.getParentCommentId());
            if (parent != null) {
                parent.replies().add(of(comment, nicknames, List.of()));
            }
        }
        return List.copyOf(topLevel.values());
    }

    private static CommentView of(Comment comment, Map<Long, String> nicknames, List<CommentView> replies) {
        boolean deleted = comment.isDeleted();
        return new CommentView(comment.getCommentId(),
                deleted ? null : nicknames.get(comment.getMemberId()),
                deleted ? null : comment.getContent(),
                comment.getCreatedAt(), comment.isEdited(), deleted, replies);
    }
}
