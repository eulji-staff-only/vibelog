package com.vibelog.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 접근 권한(비회원·학습자·운영자)과 로그인 도착지(API-USR-05 ①②⑤)를 확인한다. (#12)
 * 로그인 테스트용 회원은 테스트마다 넣고 끝나면 되돌린다(@Transactional). 비밀번호는 매번 무작위로 만든다.
 * 로컬 MySQL 이 켜져 있고 sql/01-schema.sql 로 member 테이블이 만들어져 있어야 통과한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityConfigTest {

    private static final String EMAIL = "security-test@vibelog.test";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final String password = UUID.randomUUID().toString();

    @Test
    void 비회원이_회원_화면에_오면_원래_주소를_붙여_로그인으로_보낸다() throws Exception {
        mockMvc.perform(get("/my/learning?tab=completed"))
                .andExpect(redirectedUrl("/login?redirect=%2Fmy%2Flearning%3Ftab%3Dcompleted"));
        mockMvc.perform(get("/community/write"))
                .andExpect(redirectedUrl("/login?redirect=%2Fcommunity%2Fwrite"));
    }

    @Test
    void 비회원도_전체_화면은_로그인_없이_연다() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        // 화면이 아직 없어 404일 수 있지만, 로그인으로 보내지(302) 않으면 된다
        mockMvc.perform(get("/courses/1")).andExpect(status().is(not(302)));
        mockMvc.perform(get("/community/1")).andExpect(status().is(not(302)));
    }

    @Test
    void 비회원의_JSON_요청은_401() throws Exception {
        mockMvc.perform(get("/api/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자가_백오피스에_오면_메인으로_보낸다() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(redirectedUrl("/"));
    }

    @Test
    void 로그인하면_redirect_화면으로_돌아간다() throws Exception {
        insertMember("LEARNER");

        login("/my/learning").andExpect(redirectedUrl("/my/learning"));
    }

    @Test
    void redirect가_없으면_운영자는_백오피스로_간다() throws Exception {
        insertMember("ADMIN");

        login(null).andExpect(redirectedUrl("/admin"));
    }

    @Test
    void redirect가_없으면_학습자는_코스_목록으로_간다() throws Exception {
        insertMember("LEARNER");

        login(null).andExpect(redirectedUrl("/courses"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "//evil.example", "/\\evil.example", "/\t/evil.example"})
    void 다른_사이트_주소는_redirect로_받지_않는다(String redirect) throws Exception {
        insertMember("LEARNER");

        login(redirect).andExpect(redirectedUrl("/courses"));
    }

    @Test
    void 비밀번호가_틀리면_돌아갈_주소를_유지한_채_로그인_화면으로() throws Exception {
        insertMember("LEARNER");

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", EMAIL)
                        .param("password", "wrong-" + password)
                        .param("redirect", "/my/learning"))
                .andExpect(redirectedUrl("/login?error&redirect=%2Fmy%2Flearning"));
    }

    private void insertMember(String role) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, role) VALUES (?, ?, ?, ?)",
                EMAIL, passwordEncoder.encode(password), "보안테스트", role);
    }

    private ResultActions login(String redirect) throws Exception {
        MockHttpServletRequestBuilder request = post("/login").with(csrf())
                .param("email", EMAIL)
                .param("password", password);
        if (redirect != null) {
            request.param("redirect", redirect);
        }
        return mockMvc.perform(request);
    }
}
