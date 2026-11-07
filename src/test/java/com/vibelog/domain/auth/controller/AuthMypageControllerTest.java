package com.vibelog.domain.auth.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.vibelog.domain.auth.model.service.MemberLoginService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 마이페이지(U-24)는 회원만 열고, 주소의 tab 에 맞는 탭을 보여 준다. (#51)
 * 화면에 실제 회원 정보가 들어가므로(#116) 글·댓글이 없는 회원을 넣어 로그인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthMypageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MemberLoginService memberLoginService;

    @Test
    void 비회원이_오면_로그인으로_보낸다() throws Exception {
        mockMvc.perform(get("/mypage")).andExpect(redirectedUrl("/login?redirect=%2Fmypage"));
    }

    @ParameterizedTest
    @CsvSource({
            "/mypage, 비밀번호 변경, 작성한 글이 없습니다",
            "/mypage?tab=info, 회원 탈퇴, 작성한 댓글이 없습니다",
            "/mypage?tab=posts, 작성한 글이 없습니다, 비밀번호 변경</h2>",
            "/mypage?tab=comments, 작성한 댓글이 없습니다, 비밀번호 변경</h2>"
    })
    void 탭에_맞는_내용만_보여준다(String url, String shown, String notShown) throws Exception {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES ('tab-test@vibelog.test', 'x', '탭테스트')");
        mockMvc.perform(get(url).with(user(memberLoginService.loadUserByUsername("tab-test@vibelog.test"))))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString(shown), not(containsString(notShown)))));
    }
}
