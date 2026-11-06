package com.vibelog.global.config;

import com.vibelog.domain.lesson.model.policy.LoginEnrollmentChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 접근 권한(비회원·학습자·운영자), 로그인 도착지(API-USR-05 ①②④⑤), 로그인 유지(PL-USR-006), 5회 실패 잠금(PL-USR-007)을 확인한다. (#12)
 * 로그인 테스트용 회원은 테스트마다 넣고 끝나면 되돌린다(@Transactional). 비밀번호는 매번 무작위로 만든다.
 * 계약 ⑫(진행 중 수강)는 테스트마다 값을 정하려고 가짜(Mock)로 바꿔 둔다. 기본값은 "수강 중인 코스 없음".
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

    @MockitoBean
    LoginEnrollmentChecker loginEnrollmentChecker;

    private final String password = UUID.randomUUID().toString();

    @BeforeEach
    void 수강_중인_코스가_없는_회원으로_둔다() {
        given(loginEnrollmentChecker.hasActiveEnrollment(anyLong())).willReturn(false);
    }

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
        insertMember(EMAIL, "LEARNER");

        login("/my/learning").andExpect(redirectedUrl("/my/learning"));
    }

    @Test
    void redirect가_없으면_운영자는_백오피스로_간다() throws Exception {
        insertMember(EMAIL, "ADMIN");

        login(null).andExpect(redirectedUrl("/admin"));
    }

    @Test
    void 진행_중인_수강이_있으면_내_학습으로_간다() throws Exception {
        insertMember(EMAIL, "LEARNER");
        given(loginEnrollmentChecker.hasActiveEnrollment(anyLong())).willReturn(true);

        login(null).andExpect(redirectedUrl("/my/learning"));
    }

    @Test
    void 그_밖의_학습자는_코스_목록으로_간다() throws Exception {
        insertMember(EMAIL, "LEARNER");

        login(null).andExpect(redirectedUrl("/courses"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "//evil.example", "/\\evil.example", "/\t/evil.example"})
    void 다른_사이트_주소는_redirect로_받지_않는다(String redirect) throws Exception {
        insertMember(EMAIL, "LEARNER");

        login(redirect).andExpect(redirectedUrl("/courses"));
    }

    @Test
    void 비밀번호가_틀리면_돌아갈_주소를_유지한_채_로그인_화면으로() throws Exception {
        insertMember(EMAIL, "LEARNER");

        login(EMAIL, "wrong-" + password, "/my/learning", null)
                .andExpect(redirectedUrl("/login?error&redirect=%2Fmy%2Flearning"));
    }

    @Test
    void 로그인_유지를_체크하면_14일_동안_유지하는_쿠키를_준다() throws Exception {
        insertMember(EMAIL, "LEARNER");

        login(EMAIL, password, null, "on").andExpect(cookie().maxAge("remember-me", 14 * 24 * 60 * 60));
    }

    @Test
    void 운영자는_로그인_유지를_체크해도_쿠키를_주지_않는다() throws Exception {
        insertMember(EMAIL, "ADMIN");

        login(EMAIL, password, null, "on").andExpect(cookie().doesNotExist("remember-me"));
    }

    @Test
    void 다섯_번_연속_실패하면_비밀번호가_맞아도_막는다() throws Exception {
        // 잠금은 서버 메모리에 남으므로 다른 테스트와 겹치지 않는 이메일을 쓴다
        String email = "lock-test@vibelog.test";
        insertMember(email, "LEARNER");

        for (int i = 0; i < 4; i++) {
            login(email, "wrong", null, null).andExpect(redirectedUrl("/login?error"));
        }
        // 대소문자만 바꾼 이메일도 같은 회원으로 센다
        login(email.toUpperCase(), "wrong", null, null).andExpect(redirectedUrl("/login?locked"));
        login(email, password, null, null).andExpect(redirectedUrl("/login?locked"));
    }

    private void insertMember(String email, String role) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, role) VALUES (?, ?, ?, ?)",
                email, passwordEncoder.encode(password), "보안테스트", role);
    }

    private ResultActions login(String redirect) throws Exception {
        return login(EMAIL, password, redirect, null);
    }

    private ResultActions login(String email, String password, String redirect, String rememberMe) throws Exception {
        MockHttpServletRequestBuilder request = post("/login").with(csrf())
                .param("email", email)
                .param("password", password);
        if (redirect != null) {
            request.param("redirect", redirect);
        }
        if (rememberMe != null) {
            request.param("rememberMe", rememberMe);
        }
        return mockMvc.perform(request);
    }
}
