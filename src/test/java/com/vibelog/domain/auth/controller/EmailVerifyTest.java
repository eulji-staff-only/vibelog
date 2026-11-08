package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 이메일 인증 (API-USR-03 의 인증 메일, API-USR-09·10, #89).
 * 실제 메일은 보내지 않는다. 메일 서버(JavaMailSender)를 가짜로 바꿔 보낸 메일의 링크를 꺼내 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailVerifyTest {

    private static final String EMAIL = "verify-test@vibelog.test";
    private static final Pattern TOKEN = Pattern.compile("/auth/verify\\?token=([A-Za-z0-9_-]+)");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Autowired
    MemberLoginService memberLoginService;

    @MockitoBean
    JavaMailSender javaMailSender;

    @Test
    void 가입하면_인증_메일이_가고_링크를_열면_인증된다() throws Exception {
        String token = signupAndGetToken();

        mockMvc.perform(get("/auth/verify").param("token", token))
                .andExpect(model().attribute("state", "verified"))
                .andExpect(content().string(containsString("이메일 인증이 완료되었습니다")));
        assertThat(verifiedAt()).isNotNull();

        // 한 번 쓴 링크는 다시 쓸 수 없다
        mockMvc.perform(get("/auth/verify").param("token", token)).andExpect(model().attribute("state", "expired"));
    }

    @Test
    void 하루가_지난_링크나_없는_링크는_만료_상태다() throws Exception {
        String token = signupAndGetToken();
        entityManager.flush();
        jdbcTemplate.update("UPDATE email_verification SET expires_at = expires_at - INTERVAL 25 HOUR WHERE email = ?", EMAIL);
        entityManager.clear();

        mockMvc.perform(get("/auth/verify").param("token", token))
                .andExpect(model().attribute("state", "expired"))
                .andExpect(content().string(containsString("인증 링크가 만료되었습니다")));
        mockMvc.perform(get("/auth/verify").param("token", "no-such-token")).andExpect(model().attribute("state", "expired"));
        assertThat(verifiedAt()).isNull();
    }

    @Test
    void token_없이_열면_다시_받기_상태다() throws Exception {
        mockMvc.perform(get("/auth/verify"))
                .andExpect(model().attribute("state", "resend"))
                .andExpect(content().string(containsString("data-resend-button")))
                // 비회원에게는 보냈다고 단정하지 않는다 (가입 여부를 숨김)
                .andExpect(content().string(containsString("인증 메일 요청을 받았습니다")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("인증 메일을 다시 보냈습니다"))));
    }

    @Test
    void 다시_받기는_1분_간격이고_이전_링크는_쓸_수_없다() throws Exception {
        String oldToken = signupAndGetToken();

        resendAsMember().andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("RESEND_TOO_SOON"));

        oneMinuteLater();
        clearInvocations(javaMailSender);
        resend(EMAIL).andExpect(status().isNoContent());
        String newToken = sentToken();

        mockMvc.perform(get("/auth/verify").param("token", oldToken)).andExpect(model().attribute("state", "expired"));
        mockMvc.perform(get("/auth/verify").param("token", newToken)).andExpect(model().attribute("state", "verified"));

        oneMinuteLater();
        resendAsMember().andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_VERIFIED"));
    }

    @Test
    void 비회원_요청은_1분_안이거나_이미_인증했어도_같은_성공_응답이고_메일은_가지_않는다() throws Exception {
        signupAndGetToken();
        clearInvocations(javaMailSender);

        resend(EMAIL).andExpect(status().isNoContent());   // 1분 안
        jdbcTemplate.update("UPDATE member SET email_verified_at = NOW() WHERE email = ?", EMAIL);
        oneMinuteLater();
        resend(EMAIL).andExpect(status().isNoContent());   // 이미 인증함

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void 가입하지_않은_이메일도_같은_성공_응답이고_메일은_가지_않는다() throws Exception {
        resend("nobody@vibelog.test").andExpect(status().isNoContent());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));

        resend("이메일아님").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 로그인_상태면_이메일_없이_다시_받는다() throws Exception {
        signupAndGetToken();
        oneMinuteLater();
        clearInvocations(javaMailSender);

        mockMvc.perform(post("/api/auth/verification-mail").with(user(memberLoginService.loadUserByUsername(EMAIL))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNoContent());
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    private String signupAndGetToken() throws Exception {
        mockMvc.perform(post("/signup").with(csrf())
                .param("nickname", "인증테스트")
                .param("email", EMAIL)
                .param("password", "vibelog1234"));
        return sentToken();
    }

    /* 가장 마지막에 보낸 메일에서 링크의 token 을 꺼낸다 */
    private String sentToken() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, atLeastOnce()).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly(EMAIL);
        Matcher matcher = TOKEN.matcher(message.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    /* 로그인한 본인이 다시 받기 (email 없이) */
    private ResultActions resendAsMember() throws Exception {
        return mockMvc.perform(post("/api/auth/verification-mail").with(user(memberLoginService.loadUserByUsername(EMAIL))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/verification-mail").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\": \"" + email + "\"}"));
    }

    /* 마지막 링크를 1분 전에 보낸 것으로 바꾼다 */
    private void oneMinuteLater() {
        entityManager.flush();
        jdbcTemplate.update("UPDATE email_verification SET created_at = created_at - INTERVAL 61 SECOND WHERE email = ?", EMAIL);
        entityManager.clear();
    }

    private Object verifiedAt() {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT email_verified_at FROM member WHERE email = ?", Object.class, EMAIL);
    }
}
