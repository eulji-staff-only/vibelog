package com.vibelog.domain.auth.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
}
