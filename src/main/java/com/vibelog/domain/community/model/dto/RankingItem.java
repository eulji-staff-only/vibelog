package com.vibelog.domain.community.model.dto;

/* 추천 랭킹 한 줄 (API-CMT-12 U-30: 순위, 제목, 작성자, 추천 수). 메인 화면(#203)도 CommunityReader 로 쓴다 */
public record RankingItem(int rank, Long postId, String title, String nickname, long recommendCount) {
}
