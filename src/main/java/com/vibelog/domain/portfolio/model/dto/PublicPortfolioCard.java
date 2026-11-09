package com.vibelog.domain.portfolio.model.dto;

import com.vibelog.domain.portfolio.model.entity.Portfolio;

import java.time.LocalDateTime;

/* 공개 포트폴리오 카드 한 장 (공개 프로필 U-29, API-CMT-14). community 영역이 PortfolioReader 로 받아 간다 */
public record PublicPortfolioCard(Long portfolioId, String title, String summary, LocalDateTime createdAt) {

    public static PublicPortfolioCard from(Portfolio portfolio) {
        return new PublicPortfolioCard(portfolio.getPortfolioId(), portfolio.getTitle(), portfolio.getSummary(),
                portfolio.getCreatedAt());
    }
}
