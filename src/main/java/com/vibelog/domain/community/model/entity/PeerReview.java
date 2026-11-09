package com.vibelog.domain.community.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [PeerReview]
 * 역할 : 리뷰 요청에 단 리뷰 1건 (peer_review, FR-REV-003). 코멘트는 ReviewComment 로 기준별로 남긴다.
 * 책임 : 한 요청에 1인 1리뷰 (uk_peer_review_request_reviewer, PL-REV-003). 지우면 deleted_at 만 남기고, 다시 쓰면 되살린다.
 *        "도움됨"은 요청자만 켜고 끈다 (FR-REV-004).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PeerReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long peerReviewId;

    private Long reviewRequestId;

    private Long reviewerMemberId;

    private LocalDateTime helpfulAt;

    private LocalDateTime deletedAt;

    private PeerReview(Long reviewRequestId, Long reviewerMemberId) {
        this.reviewRequestId = reviewRequestId;
        this.reviewerMemberId = reviewerMemberId;
    }

    public static PeerReview create(Long reviewRequestId, Long reviewerMemberId) {
        return new PeerReview(reviewRequestId, reviewerMemberId);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isWrittenBy(Long memberId) {
        return reviewerMemberId.equals(memberId);
    }

    public void delete() {
        deletedAt = LocalDateTime.now();
    }

    /* 지운 리뷰를 다시 쓸 때 (명세: 지운 행을 되살린다). 도움됨 표시는 새 리뷰처럼 비운다 */
    public void revive() {
        deletedAt = null;
        helpfulAt = null;
    }

    /* 바꾼 뒤 도움됨 상태를 돌려준다 */
    public boolean toggleHelpful() {
        helpfulAt = helpfulAt == null ? LocalDateTime.now() : null;
        return helpfulAt != null;
    }
}
