package com.vibelog.domain.portfolio.model.dto;

import jakarta.validation.constraints.NotNull;

/*
 * [PortfolioVisibilityRequest]
 * 역할 : 공개 전환 요청 JSON {"isPublic": true|false} (API-PRT-08).
 */
public record PortfolioVisibilityRequest(@NotNull Boolean isPublic) {
}
