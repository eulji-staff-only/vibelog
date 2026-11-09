package com.vibelog.domain.community.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [ReviewComment]
 * 역할 : 리뷰의 평가 기준별 코멘트 (review_comment). 기준이 없는 과제·개인 작품은 reviewCriterionId 없이 1개.
 * 책임 : 리뷰를 고치면 코멘트를 지우고 새로 넣는다 (PeerReviewService).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewCommentId;

    private Long peerReviewId;

    private Long reviewCriterionId;

    private String content;

    private ReviewComment(Long peerReviewId, Long reviewCriterionId, String content) {
        this.peerReviewId = peerReviewId;
        this.reviewCriterionId = reviewCriterionId;
        this.content = content;
    }

    public static ReviewComment create(Long peerReviewId, Long reviewCriterionId, String content) {
        return new ReviewComment(peerReviewId, reviewCriterionId, content);
    }
}
