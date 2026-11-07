package com.vibelog.domain.auth.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 비밀번호 찾기·재설정 (API-USR-11~14, #90). 실제 메일은 보내지 않고 가짜 메일 서버로 보낸 메일을 꺼내 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetTest {

    private static final String EMAIL = "reset-test@vibelog.test";
    private static final String OLD_PASSWORD = "oldpass1234";
    private static final String NEW_PASSWORD = "newpass5678";
    private static final Pattern TOKEN = Pattern.compile("/password/reset\\?token=([A-Za-z0-9_-]+)");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    JavaMailSender javaMailSender;

    @BeforeEach
    void 이메일_가입_회원을_넣는다() {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, ?, '재설정러')",
                EMAIL, passwordEncoder.encode(OLD_PASSWORD));
    }

    @Test
    void 메일의_링크로_새_비밀번호를_정하면_로그인_화면으로_간다() throws Exception {
        find(EMAIL).andExpect(redirectedUrl("/password/find?sent=true"));
        String token = sentToken();

        mockMvc.perform(get("/password/reset").param("token", token))
                .andExpect(model().attributeDoesNotExist("expired"))
                .andExpect(content().string(containsString("name=\"token\" value=\"" + token + "\"")));
        reset(token, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("toast", "비밀번호가 변경되었습니다"));

        assertThat(passwordEncoder.matches(NEW_PASSWORD, passwordHash())).isTrue();
        // 링크는 한 번만 쓸 수 있다
        mockMvc.perform(get("/password/reset").param("token", token)).andExpect(model().attribute("expired", true));
        reset(token, "another1234", "another1234").andExpect(model().attribute("expired", true));
        assertThat(passwordEncoder.matches(NEW_PASSWORD, passwordHash())).isTrue();
    }

    @Test
    void 삼십분이_지난_링크는_만료_상태다() throws Exception {
        find(EMAIL);
        String token = sentToken();
        entityManager.flush();
        jdbcTemplate.update("UPDATE email_verification SET expires_at = expires_at - INTERVAL 31 MINUTE WHERE email = ?", EMAIL);
        entityManager.clear();

        mockMvc.perform(get("/password/reset").param("token", token))
                .andExpect(model().attribute("expired", true))
                .andExpect(content().string(containsString("링크가 만료되었습니다")));
        reset(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(model().attribute("expired", true));
        assertThat(passwordEncoder.matches(OLD_PASSWORD, passwordHash())).isTrue();
    }

    @Test
    void 비밀번호_규칙이나_확인이_틀리면_같은_폼에_보여준다() throws Exception {
        find(EMAIL);
        String token = sentToken();

        reset(token, "short1", "short1")
                .andExpect(view().name("auth/password-reset"))
                .andExpect(model().attributeHasFieldErrors("passwordResetRequest", "newPassword"));
        reset(token, NEW_PASSWORD, "different99")
                .andExpect(model().attributeHasFieldErrors("passwordResetRequest", "newPasswordConfirmed"))
                .andExpect(content().string(containsString("새 비밀번호가 서로 다릅니다")));

        assertThat(passwordEncoder.matches(OLD_PASSWORD, passwordHash())).isTrue();
        // 입력 오류로는 링크가 쓰이지 않는다
        reset(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(redirectedUrl("/login"));
    }

    @Test
    void 가입하지_않은_이메일과_소셜_가입_계정도_같은_화면이고_소셜은_안내_메일만_간다() throws Exception {
        find("nobody@vibelog.test").andExpect(redirectedUrl("/password/find?sent=true"));
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));

        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES ('social-reset@vibelog.test', NULL, '소셜러', NOW())");
        find("social-reset@vibelog.test").andExpect(redirectedUrl("/password/find?sent=true"));
        SimpleMailMessage mail = lastMail();
        assertThat(mail.getText()).contains("구글·깃허브").doesNotContain("token=");
    }

    @Test
    void 일분_안에_다시_요청하면_메일을_다시_보내지_않는다() throws Exception {
        find(EMAIL);
        find(EMAIL).andExpect(redirectedUrl("/password/find?sent=true"));

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void 이메일_형식이_틀리면_같은_폼에_보여준다() throws Exception {
        find("이메일아님")
                .andExpect(view().name("auth/password-find"))
                .andExpect(model().attributeHasFieldErrors("passwordFindRequest", "email"));
    }

    private ResultActions find(String email) throws Exception {
        return mockMvc.perform(post("/password/find").with(csrf()).param("email", email));
    }

    private ResultActions reset(String token, String newPassword, String confirm) throws Exception {
        return mockMvc.perform(post("/password/reset").with(csrf())
                .param("token", token).param("newPassword", newPassword).param("newPasswordConfirm", confirm));
    }

    private SimpleMailMessage lastMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(captor.capture());
        return captor.getValue();
    }

    private String sentToken() {
        SimpleMailMessage mail = lastMail();
        assertThat(mail.getTo()).containsExactly(EMAIL);
        Matcher matcher = TOKEN.matcher(mail.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String passwordHash() {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT password_hash FROM member WHERE email = ?", String.class, EMAIL);
    }
}
