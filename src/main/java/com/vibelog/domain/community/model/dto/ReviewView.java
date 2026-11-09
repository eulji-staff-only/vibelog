package com.vibelog.domain.community.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/* U-20 받은 리뷰 한 건 (CommunityReader.findReviewsOfPortfolio). mine 이면 수정·삭제, 작품 주인이면 도움됨 버튼 */
public record ReviewView(Long peerReviewId, Long reviewerMemberId, String reviewerNickname, LocalDateTime createdAt, boolean helpful,
                         boolean mine, List<Comment> comments) {

    /* criterionName 은 기준 없는 코멘트면 null */
    public record Comment(String criterionName, String content) {
    }
}
