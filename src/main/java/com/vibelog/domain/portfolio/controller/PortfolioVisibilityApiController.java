package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.portfolio.model.dto.PortfolioVisibilityRequest;
import com.vibelog.domain.portfolio.model.dto.PortfolioVisibilityResult;
import com.vibelog.domain.portfolio.model.service.PortfolioManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
 * [PortfolioVisibilityApiController]
 * 역할 : 내 포트폴리오(U-19)의 공개 전환(API-PRT-08, JSON)을 받는다.
 * 책임 : 새로고침 없이 바뀌는 동작이라 JSON {isPublic} 으로 답한다.
 *        실패(NOT_OWNER 등)는 GlobalExceptionHandler 가 {code, message} 로 보낸다.
 */
@RestController
@RequiredArgsConstructor
public class PortfolioVisibilityApiController {

    private final PortfolioManageService portfolioManageService;

    @PostMapping("/api/portfolios/{portfolioId}/visibility")
    public PortfolioVisibilityResult changeVisibility(@PathVariable Long portfolioId,
                                                      @Valid @RequestBody PortfolioVisibilityRequest request,
                                                      @AuthenticationPrincipal LoginMember loginMember) {
        return new PortfolioVisibilityResult(
                portfolioManageService.changeVisibility(loginMember.getMemberId(), portfolioId, request.isPublic()));
    }
}
