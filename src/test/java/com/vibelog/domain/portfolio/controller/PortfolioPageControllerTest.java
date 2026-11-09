package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.GuideStepCheckResult;
import com.vibelog.domain.portfolio.model.dto.PortfolioPageView;
import com.vibelog.domain.portfolio.model.service.PortfolioService;
import com.vibelog.global.exception.BusinessException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 포트폴리오 과제 화면·설정 파일·스텝 체크 요청(API-PRT-01~03)이 로그인 회원의 memberId 만 Service 에 넘기고,
 * 결과에 맞는 화면·redirect·파일·JSON 으로 답하는지 확인한다 (DB 없이 도는 슬라이스 테스트).
 * 템플릿이 실제 데이터로 오류 없이 그려지는지도 여기서 잡는다.
 */
@WebMvcTest({PortfolioPageController.class, PortfolioGuideStepApiController.class})
class PortfolioPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PortfolioService portfolioService;

    private RequestPostProcessor login() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }

    private PortfolioPageView openPage(Long submittedPortfolioId, boolean emailVerified) {
        return new PortfolioPageView(3L, "바이브 코스", true, null, "할 일 앱 만들기", "목표", "세팅 방법 안내", true,
                List.of(new PortfolioPageView.Step(11L, "첫 스텝", true), new PortfolioPageView.Step(12L, "둘째 스텝", false)),
                1, submittedPortfolioId, emailVerified);
    }

    @Test
    void 열린_과제는_과제_스텝_안내와_제출_양식을_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, true)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/portfolio"))
                .andExpect(content().string(Matchers.containsString("할 일 앱 만들기")))
                .andExpect(content().string(Matchers.containsString("둘째 스텝")))
                .andExpect(content().string(Matchers.containsString("/api/guide-steps/11/check")))
                .andExpect(content().string(Matchers.containsString("/courses/3/portfolio/agent-file")))
                .andExpect(content().string(Matchers.containsString("세팅 방법 안내")))
                .andExpect(content().string(Matchers.containsString("data-pf-form")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("아직 열리지 않았습니다"))));
    }

    @Test
    void 이메일_미인증이면_제출_양식_대신_인증_안내를_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, false)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(content().string(Matchers.containsString("이메일 인증 후 포트폴리오를 제출할 수 있습니다.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-pf-form"))));
    }

    @Test
    void 이미_제출했으면_내_포트폴리오_링크를_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(21L, true)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(content().string(Matchers.containsString("/portfolios/21")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-pf-form"))));
    }

    @Test
    void 잠긴_과제는_리포트_안내만_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(PortfolioPageView.locked(3L, "바이브 코스", 7L)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("아직 열리지 않았습니다")))
                .andExpect(content().string(Matchers.containsString("/lessons/7/exam/report")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("포트폴리오 제출"))));
    }

    @Test
    void 수강하지_않은_코스면_코스_소개로_보낸다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.empty());

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(redirectedUrl("/courses/3"));
    }

    @Test
    void 설정_파일은_내려받기_헤더를_붙여_저장된_파일로_넘긴다() throws Exception {
        given(portfolioService.getAgentFileUrl(1L, 3L)).willReturn("/uploads/abc.md");

        mockMvc.perform(get("/courses/3/portfolio/agent-file").with(login()))
                .andExpect(forwardedUrl("/uploads/abc.md"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"agent-file.md\""));
    }

    @Test
    void 잠긴_과제의_설정_파일은_내려받을_수_없다() throws Exception {
        given(portfolioService.getAgentFileUrl(1L, 3L)).willThrow(new BusinessException(PortfolioErrorCode.PORTFOLIO_LOCKED));

        mockMvc.perform(get("/courses/3/portfolio/agent-file").with(login()))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/error"));
    }

    @Test
    void 스텝_체크는_JSON으로_완료_수를_돌려준다() throws Exception {
        given(portfolioService.check(1L, 11L, true)).willReturn(new GuideStepCheckResult(2, 3));

        mockMvc.perform(post("/api/guide-steps/11/check").with(login()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checked\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedCount").value(2))
                .andExpect(jsonPath("$.totalCount").value(3));
    }

    @Test
    void 잠긴_과제의_스텝_체크는_JSON_오류로_답한다() throws Exception {
        given(portfolioService.check(1L, 11L, true)).willThrow(new BusinessException(PortfolioErrorCode.PORTFOLIO_LOCKED));

        mockMvc.perform(post("/api/guide-steps/11/check").with(login()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checked\": true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PORTFOLIO_LOCKED"));
    }
}
