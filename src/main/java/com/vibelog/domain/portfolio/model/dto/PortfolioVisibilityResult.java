package com.vibelog.domain.portfolio.model.dto;

/*
 * [PortfolioVisibilityResult]
 * 역할 : 공개 전환 응답 JSON {"isPublic": true|false} (API-PRT-08).
 */
public record PortfolioVisibilityResult(boolean isPublic) {
}
