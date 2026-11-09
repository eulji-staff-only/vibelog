package com.vibelog.domain.community.model.dto;

import com.vibelog.domain.community.model.entity.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * 게시글 상세의 댓글 하나와 그 답글 (API-CMT-02).
 * 삭제된 댓글은 내용·작성자를 보여 주지 않는다 ("삭제된 댓글입니다", PL-CMT-006).
 * mine 이면 수정·삭제 버튼을 보여 준다 (삭제된 댓글은 아님). 검사는 CommentService 가 다시 한다.
 */
public record CommentView(Long commentId, String nickname, String content, LocalDateTime createdAt,
                          boolean edited, boolean deleted, boolean mine, List<CommentView> replies, boolean hidden) {

    /* 작성 순서대로 받은 댓글을 "댓글 → 그 아래 답글" 모양으로 묶는다. 답글은 한 단계까지만 있다 */
    /* hiddenMemberIds(영구 정지 회원, PL-RPT-006)의 댓글은 자리만 남기고 작성자·내용을 숨긴다 (#215) */
    public static List<CommentView> tree(List<Comment> comments, Map<Long, String> nicknames, Long loginMemberId,
                                         Set<Long> hiddenMemberIds) {
        Map<Long, CommentView> topLevel = new LinkedHashMap<>();
        for (Comment comment : comments) {
            if (comment.getParentCommentId() == null) {
                topLevel.put(comment.getCommentId(), of(comment, nicknames, loginMemberId, hiddenMemberIds, new ArrayList<>()));
            }
        }
        for (Comment comment : comments) {
            CommentView parent = comment.getParentCommentId() == null ? null : topLevel.get(comment.getParentCommentId());
            if (parent != null) {
                parent.replies().add(of(comment, nicknames, loginMemberId, hiddenMemberIds, List.of()));
            }
        }
        return List.copyOf(topLevel.values());
    }

    private static CommentView of(Comment comment, Map<Long, String> nicknames, Long loginMemberId, Set<Long> hiddenMemberIds,
                                  List<CommentView> replies) {
        boolean deleted = comment.isDeleted();
        boolean hidden = !deleted && hiddenMemberIds.contains(comment.getMemberId());
        boolean shown = !deleted && !hidden;
        return new CommentView(comment.getCommentId(),
                shown ? nicknames.get(comment.getMemberId()) : null,
                shown ? comment.getContent() : null,
                comment.getCreatedAt(), comment.isEdited(), deleted,
                shown && comment.isWrittenBy(loginMemberId), replies, hidden);
    }
}
