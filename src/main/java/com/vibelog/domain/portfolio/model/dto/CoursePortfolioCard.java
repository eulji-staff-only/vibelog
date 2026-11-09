package com.vibelog.domain.portfolio.model.dto;

import java.time.LocalDateTime;

/*
 * [CoursePortfolioCard]
 * 역할 : U-35 같은 코스 작품 보기의 카드 한 장 (API-PRT-10, FR-PRT-009).
 * 책임 : 제목, 한 줄 요약, 작성자(MemberReader 닉네임), 제출일. 카드를 누르면 U-20 상세로 간다.
 */
public record CoursePortfolioCard(Long portfolioId, String title, String summary, String authorNickname,
                                  LocalDateTime submittedAt) {
}
