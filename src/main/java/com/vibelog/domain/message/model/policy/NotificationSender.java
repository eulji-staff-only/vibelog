package com.vibelog.domain.message.model.policy;

/*
 * [NotificationSender] 계약 ⑬ (community·contest 가 부름)
 * 역할 : 다른 영역에서 생긴 일(추천·댓글·답글·리뷰·배지)을 받는 회원의 알림으로 남긴다 (FR-NTF-001).
 * 책임 : - 알림 문구는 여기서 만든다. 부르는 쪽은 문구를 만들지 않는다.
 *        - actorMemberId(행동한 사람)와 receiverMemberId 가 같으면 남기지 않는다 (내 글에 내가 댓글 등).
 *        - 부르는 쪽 @Transactional 안에서 부른다 (08 문서 규칙 1-6).
 *        명세: docs/design/api/message.md "영역 간 계약"
 */
public interface NotificationSender {

    /* RECOMMEND · POST */
    void recommended(Long actorMemberId, Long receiverMemberId, Long postId, String postTitle);

    /* COMMENT · COMMENT */
    void commented(Long actorMemberId, Long receiverMemberId, Long commentId, String postTitle);

    /* REPLY · COMMENT */
    void replied(Long actorMemberId, Long receiverMemberId, Long commentId, String postTitle);

    /* REVIEW · REVIEW (target 은 peer_review_id) */
    void reviewed(Long actorMemberId, Long receiverMemberId, Long peerReviewId, String portfolioTitle);

    /* BADGE · CONTEST_AWARD (시스템이 주므로 행동한 사람이 없다) */
    void badgeAwarded(Long receiverMemberId, Long contestAwardId, String badgeName);
}
