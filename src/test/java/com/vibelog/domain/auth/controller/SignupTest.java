package com.vibelog.domain.auth.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 이메일 회원가입 (API-USR-02·03, #88). 넣은 회원·금칙어는 테스트가 끝나면 되돌린다(@Transactional).
 * 로컬 MySQL 이 켜져 있고 sql/01-schema.sql 로 테이블이 만들어져 있어야 통과한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SignupTest {

    private static final String EMAIL = "signup-test@vibelog.test";
    private static final String PASSWORD = "vibelog1234";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void 가입하면_비밀번호를_해시로_저장하고_로그인된_채_가입_완료로_간다() throws Exception {
        signup("가입테스트", EMAIL, PASSWORD)
                .andExpect(redirectedUrl("/signup?done=true"))
                .andExpect(authenticated().withUsername(EMAIL).withRoles("LEARNER"));

        String hash = jdbcTemplate.queryForObject("SELECT password_hash FROM member WHERE email = ?", String.class, EMAIL);
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
    }

    @Test
    void 이미_가입된_이메일이면_같은_폼에_로그인_링크와_입력값을_남긴다() throws Exception {
        insertMember("기존회원", EMAIL);

        signup("새회원", EMAIL.toUpperCase(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrorCode("signupRequest", "email", "DUPLICATE_EMAIL"))
                .andExpect(content().string(containsString("이미 가입된 이메일입니다. 로그인해 주세요.")))
                .andExpect(content().string(containsString("value=\"새회원\"")))
                .andExpect(content().string(not(containsString(PASSWORD))))
                .andExpect(unauthenticated());
    }

    @Test
    void 이미_쓰는_닉네임이면_막는다() throws Exception {
        insertMember("겹치는닉", "other-signup@vibelog.test");

        signup("겹치는닉", EMAIL, PASSWORD)
                .andExpect(model().attributeHasFieldErrorCode("signupRequest", "nickname", "DUPLICATE_NICKNAME"));
        assertThat(memberCount(EMAIL)).isZero();
    }

    @Test
    void 금칙어가_든_닉네임은_막는다() throws Exception {
        jdbcTemplate.update("INSERT INTO banned_word (word) VALUES (?)", "가입금칙");

        signup("나는가입금칙어", EMAIL, PASSWORD)
                .andExpect(model().attributeHasFieldErrorCode("signupRequest", "nickname", "BANNED_WORD"));
        assertThat(memberCount(EMAIL)).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "가, nickname",                       // 2자 미만
            "열세글자가넘는아주긴닉네임이야, nickname", // 12자 초과
            "공백 있음, nickname",
            "특수문자!, nickname",
            "탈퇴한 사용자, nickname",
            "정상닉네임, email"
    })
    void 닉네임_규칙과_이메일_형식에_안_맞으면_저장하지_않는다(String nickname, String field) throws Exception {
        String email = field.equals("email") ? "not-an-email" : EMAIL;

        signup(nickname, email, PASSWORD)
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("signupRequest", field));
        assertThat(memberCount(email)).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "abcd123",          // 8자 미만
            "abcdefgh",         // 숫자 없음
            "12345678",         // 영문 없음
            "비밀번호abc123",     // 한글
            "abc 12345"         // 공백
    })
    void 비밀번호_규칙에_안_맞으면_저장하지_않는다(String password) throws Exception {
        signup("가입테스트", EMAIL, password)
                .andExpect(model().attributeHasFieldErrors("signupRequest", "password"));
        assertThat(memberCount(EMAIL)).isZero();
    }

    @Test
    void 비밀번호는_64자까지_받는다() throws Exception {
        signup("가입테스트", EMAIL, "a1".repeat(32)).andExpect(redirectedUrl("/signup?done=true"));
        signup("가입테스트2", "signup-test2@vibelog.test", "a1".repeat(32) + "x")
                .andExpect(model().attributeHasFieldErrors("signupRequest", "password"));
    }

    @Test
    @WithMockUser
    void 로그인한_회원이_가입_화면에_오면_메인으로_보내고_가입_완료는_보여준다() throws Exception {
        mockMvc.perform(get("/signup")).andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/signup?done=true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("가입이 완료되었습니다")));
    }

    private ResultActions signup(String nickname, String email, String password) throws Exception {
        return mockMvc.perform(post("/signup").with(csrf())
                .param("nickname", nickname)
                .param("email", email)
                .param("password", password));
    }

    private void insertMember(String nickname, String email) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, ?, ?)",
                email, passwordEncoder.encode(PASSWORD), nickname);
    }

    private int memberCount(String email) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member WHERE email = ?", Integer.class, email);
    }
}
