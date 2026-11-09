package com.vibelog.domain.community.model.dto;

import java.util.List;

/* U-31 리뷰 쓰기·수정 폼 (API-REV-02 reviewRequestId). peerReviewId 가 있으면 수정 */
public record ReviewFormView(Long reviewRequestId, Long portfolioId, String portfolioTitle, Long peerReviewId, List<ReviewField> fields) {

    public boolean editing() {
        return peerReviewId != null;
    }
}
