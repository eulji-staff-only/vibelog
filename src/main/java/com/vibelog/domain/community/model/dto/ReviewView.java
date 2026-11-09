package com.vibelog.domain.community.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * U-20 받은 리뷰 한 건 (CommunityReader.findReviewsOfPortfolio). mine 이면 수정·삭제, 작품 주인이면 도움됨 버튼.
 * hidden 이면 영구 정지 회원의 리뷰(#215)나 신고로 숨긴 남의 리뷰(#256)라 닉네임·코멘트가 비어 있다.
 * 화면은 "운영 정책에 따라 숨겨진 콘텐츠입니다" (PL-ERR-006). hiddenNotice 는 신고로 숨긴 내 리뷰라 내용 + 숨김 안내 (FR-RPT-003)
 */
public record ReviewView(Long peerReviewId, Long reviewerMemberId, String reviewerNickname, LocalDateTime createdAt, boolean helpful,
                         boolean mine, List<Comment> comments, boolean hidden, boolean hiddenNotice) {

    /* criterionName 은 기준 없는 코멘트면 null */
    public record Comment(String criterionName, String content) {
    }
}
