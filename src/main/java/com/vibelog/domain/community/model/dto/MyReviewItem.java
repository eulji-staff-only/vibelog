package com.vibelog.domain.community.model.dto;

import java.time.LocalDateTime;

/* U-31 내가 쓴 리뷰 한 줄. 작품이 비공개·삭제됐으면 portfolioTitle 은 "비공개된 작품" (명세 API-REV-02) */
public record MyReviewItem(Long peerReviewId, Long reviewRequestId, String portfolioTitle, LocalDateTime createdAt) {

    public static final String HIDDEN_TITLE = "비공개된 작품";
}
