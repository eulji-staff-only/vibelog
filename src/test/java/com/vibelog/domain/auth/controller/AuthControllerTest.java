package com.vibelog.domain.auth.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 회원 화면 5개(U-02~U-06)가 비회원에게 설계서 URL 로 열리는지 확인한다. (#49)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
            "/signup, auth/signup, 회원가입",
            "/login, auth/login, 로그인",
            "/auth/verify?token=sample, auth/verify, 이메일 인증",
            "/password/find, auth/password-find, 비밀번호 찾기",
            "/password/reset?token=sample, auth/password-reset, 비밀번호 재설정"
    })
    void 비회원도_회원_화면을_연다(String url, String viewName, String title) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName))
                .andExpect(content().string(containsString("<h1 class=\"au-title\">" + title + "</h1>")));
    }

    /* 처음 온 비회원도 redirect 로 오므로 "로그아웃되었습니다"라고 하지 않는다 (#163) */
    @Test
    void 회원_화면에서_보내진_로그인은_로그인이_필요하다고_안내한다() throws Exception {
        mockMvc.perform(get("/login").param("redirect", "/mypage"))
                .andExpect(content().string(allOf(containsString("로그인이 필요한 화면입니다. 로그인하면 보던 화면으로 돌아갑니다"),
                        not(containsString("로그아웃되었습니다")))));
    }

    /* 로그인한 회원은 로그인 폼 대신 메인으로 간다. 로그인 뒤 뒤로 가기도 같은 요청이다 (#393) */
    @Test
    @WithMockUser
    void 로그인한_회원이_로그인_화면을_열면_메인으로_간다() throws Exception {
        mockMvc.perform(get("/login")).andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/login").param("redirect", "/mypage")).andExpect(redirectedUrl("/"));
        // 로그인한 채 비밀번호를 재설정하면 /login 으로 온 완료 안내를 메인까지 넘긴다
        mockMvc.perform(get("/login").flashAttr("toast", "비밀번호가 변경되었습니다"))
                .andExpect(redirectedUrl("/")).andExpect(flash().attribute("toast", "비밀번호가 변경되었습니다"));
    }

    /* 잠금 남은 시간 (#394). 풀리는 시각(until)으로 계산하고, 주소를 바꿔도 5분을 넘게 보이지 않는다 */
    @Test
    void 잠금_안내는_풀리는_시각으로_남은_시간을_보여준다() throws Exception {
        long now = Instant.now().getEpochSecond();
        mockMvc.perform(get("/login").param("locked", "").param("until", String.valueOf(now + 100_000)))
                .andExpect(content().string(allOf(containsString("data-lock-seconds=\"300\""), containsString("5:00"),
                        not(containsString("5분 뒤에 다시 시도해 주세요")))));
        mockMvc.perform(get("/login").param("locked", "").param("until", String.valueOf(now - 10)))
                .andExpect(content().string(containsString("data-lock-seconds=\"0\"")));
        // until 이 없는 예전 주소는 고정 안내
        mockMvc.perform(get("/login").param("locked", ""))
                .andExpect(content().string(containsString("5분 뒤에 다시 시도해 주세요")));
    }
}
