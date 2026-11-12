package com.vibelog.domain.community.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dao.CommentRepository;
import com.vibelog.domain.community.model.dao.PostRepository;
import com.vibelog.domain.community.model.entity.Comment;
import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.message.model.policy.NotificationSender;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import com.vibelog.global.policy.BannedWordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [CommentService]
 * 역할 : 댓글·답글 쓰기, 댓글 수정·삭제 (API-CMT-08~10, FR-CMT-004).
 * 책임 : - 쓰기는 이메일 인증한 회원만 (PL-USR-003, EMAIL_NOT_VERIFIED), 수정·삭제는 작성자만 (NOT_OWNER, NFR-SEC-003).
 *        - 답글은 댓글에만 단다. 답글에 답글은 REPLY_DEPTH_EXCEEDED (PL-CMT-006). 금칙어는 BANNED_WORD (PL-SEC-003).
 *        - 없거나 삭제된 글·댓글, 다른 글의 댓글·삭제된 댓글에 단 답글은 POST_NOT_FOUND (7장 표에 댓글용 이름이 없어 같은 404 를 쓴다).
 *        - 삭제는 deleted_at 만 채운다. 답글이 달린 댓글도 "삭제된 댓글입니다"로 자리가 남는다 (PL-CMT-006, PL-DAT-002).
 *        - 수정·삭제는 댓글이 달린 글 ID 를 돌려준다. 끝나면 그 글로 돌아가기 때문이다.
 *        - 쓰면 알림 (계약 ⑬, #185): 댓글은 글쓴이에게 COMMENT, 답글은 댓글쓴이에게 REPLY. 나에게는 계약 쪽이 보내지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberReader memberReader;
    private final BannedWordPolicy bannedWordPolicy;
    private final NotificationSender notificationSender;

    @Transactional
    public Long write(Long postId, Long memberId, String content, Long parentCommentId) {
        Post post = postRepository.findByPostIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
        if (!memberReader.isEmailVerified(memberId)) {
            throw new BusinessException(CommonErrorCode.EMAIL_NOT_VERIFIED);
        }
        Comment parent = null;
        if (parentCommentId != null) {
            parent = commentRepository.findById(parentCommentId)
                    .filter(comment -> comment.getPostId().equals(postId) && !comment.isDeleted())
                    .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
            if (parent.isReply()) {
                throw new BusinessException(CommunityErrorCode.REPLY_DEPTH_EXCEEDED);
            }
        }
        bannedWordPolicy.check(content);
        Long commentId = commentRepository.save(Comment.create(postId, memberId, parentCommentId, content.strip())).getCommentId();
        if (parent == null) {
            notificationSender.commented(memberId, post.getMemberId(), commentId, post.getTitle());
        } else {
            notificationSender.replied(memberId, parent.getMemberId(), commentId, post.getTitle());
        }
        return commentId;
    }

    @Transactional
    public Long edit(Long commentId, Long memberId, String content) {
        Comment comment = getOwnComment(commentId, memberId);
        bannedWordPolicy.check(content);
        comment.edit(content.strip());
        return comment.getPostId();
    }

    @Transactional
    public Long delete(Long commentId, Long memberId) {
        Comment comment = getOwnComment(commentId, memberId);
        comment.delete();
        return comment.getPostId();
    }

    /* 수정 폼 입력 오류로 글에 돌아갈 때 쓴다. 댓글은 누구나 보는 글에 있으므로 작성자는 보지 않는다 */
    public Long getPostId(Long commentId) {
        return commentRepository.findById(commentId)
                .map(Comment::getPostId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
    }

    /* 글이 삭제됐으면 그 글의 댓글도 고칠 수 없다 */
    private Comment getOwnComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(found -> !found.isDeleted())
                .filter(found -> postRepository.findByPostIdAndDeletedAtIsNull(found.getPostId()).isPresent())
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
        if (!comment.isWrittenBy(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        return comment;
    }
}
