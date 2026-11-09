package com.vibelog.domain.community.model.dto;

import java.time.LocalDateTime;

/* U-31 열린 리뷰 요청 카드 (작품 제목, 한 줄 요약, 코스, 작성자, 요청일, 받은 리뷰 수) */
public record ReviewRequestCard(Long reviewRequestId, Long portfolioId, String title, String summary, String courseTitle,
                                String nickname, LocalDateTime requestedAt, long reviewCount) {
}
