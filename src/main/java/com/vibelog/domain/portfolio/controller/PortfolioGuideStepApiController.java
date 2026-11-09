package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.portfolio.model.dto.GuideStepCheckRequest;
import com.vibelog.domain.portfolio.model.dto.GuideStepCheckResult;
import com.vibelog.domain.portfolio.model.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
 * [PortfolioGuideStepApiController]
 * 역할 : 스텝 체크 자동 저장(API-PRT-02, JSON)을 받는다.
 * 책임 : 화면이 바뀌지 않는 동작이라 JSON {checkedCount, totalCount} 로 답한다.
 *        실패(PORTFOLIO_LOCKED 등)는 GlobalExceptionHandler 가 {code, message} 로 보낸다.
 */
@RestController
@RequiredArgsConstructor
public class PortfolioGuideStepApiController {

    private final PortfolioService portfolioService;

    @PostMapping("/api/guide-steps/{guideStepId}/check")
    public GuideStepCheckResult check(@PathVariable Long guideStepId,
                                      @Valid @RequestBody GuideStepCheckRequest request,
                                      @AuthenticationPrincipal LoginMember loginMember) {
        return portfolioService.check(loginMember.getMemberId(), guideStepId, request.checked());
    }
}
