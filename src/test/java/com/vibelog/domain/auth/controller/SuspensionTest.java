package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.service.MemberLoginService;
import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.auth.model.service.MypageService;
import com.vibelog.global.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 이용 정지·해제 (FR-USR-010, API-USR-19·20, PL-RPT-005·006, PL-USR-010, #178).
 * 폼 로그인·로그인 유지 쿠키·로그인 중 POST·탈퇴에서 정지를 막는지 본다. 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuspensionTest {

    private static final String LEARNER = "suspend-learner@vibelog.test";
    private static final String ADMIN = "suspend-admin@vibelog.test";
    private static final String PASSWORD = "pass1234";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MemberLoginService memberLoginService;
    @Autowired MypageService mypageService;
    @Autowired MemberReader memberReader;

    private long learnerId;
    private long adminId;

    @BeforeEach
    void 학습자와_운영자를_넣는다() {
        String hash = passwordEncoder.encode(PASSWORD);
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, ?, '정지대상', NOW())",
                LEARNER, hash);
        learnerId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, role, email_verified_at) VALUES (?, ?, '정지운영자', 'ADMIN', NOW())",
                ADMIN, hash);
        adminId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void 운영자가_7일_정지하면_상세에_보이고_끝나는_시각이_남는다() throws Exception {
        mockMvc.perform(post("/admin/members/{id}/suspend", learnerId).with(as(ADMIN)).with(csrf())
                        .param("level", "DAYS_7").param("reason", "도배"))
                .andExpect(redirectedUrl("/admin/members/" + learnerId))
                .andExpect(flash().attribute("toast", "제재를 저장했습니다"));

        LocalDateTime endsAt = jdbcTemplate.queryForObject(
                "SELECT ends_at FROM member_suspension WHERE member_id = ?", LocalDateTime.class, learnerId);
        assertThat(endsAt).isAfter(LocalDateTime.now().plusDays(6)).isBefore(LocalDateTime.now().plusDays(8));
        mockMvc.perform(get("/admin/members/{id}", learnerId).with(as(ADMIN)))
                .andExpect(content().string(allOf(containsString("7일 정지 중"), containsString("도배"), containsString("정지 해제"))));
    }

    @Test
    void 정지된_회원은_비밀번호가_맞을_때만_M07이_뜬다() throws Exception {
        suspend("DAYS_30", "욕설", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(29));

        MvcResult result = mockMvc.perform(post("/login").with(csrf()).param("email", LEARNER).param("password", PASSWORD))
                .andExpect(redirectedUrl("/login?suspended"))
                .andReturn();
        // M-07 값은 세션 flash 로 넘어가므로(SuspendedLoginResponder) 같은 세션으로 로그인 화면을 연다
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/login").param("suspended", "").session(session))
                .andExpect(content().string(allOf(containsString("이용이 정지된 계정입니다"), containsString("30일 정지"),
                        containsString("욕설"))));

        mockMvc.perform(post("/login").with(csrf()).param("email", LEARNER).param("password", "wrong999"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void 경고와_기간이_끝난_정지는_로그인을_막지_않는다() throws Exception {
        suspend("WARNING", "주의", LocalDateTime.now().minusDays(1), null);
        suspend("DAYS_7", "지난 정지", LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(3));

        mockMvc.perform(post("/login").with(csrf()).param("email", LEARNER).param("password", PASSWORD))
                .andExpect(redirectedUrl("/courses"));
    }

    @Test
    void 로그인한_채로_정지되면_POST는_막히고_보기는_된다() throws Exception {
        RequestPostProcessor learner = as(LEARNER);
        suspend("PERMANENT", "영구", LocalDateTime.now(), null);

        mockMvc.perform(get("/mypage").with(learner)).andExpect(status().isOk());
        mockMvc.perform(post("/mypage/password").with(learner).with(csrf())
                        .param("currentPassword", PASSWORD).param("newPassword", "newpass5678"))
                .andExpect(redirectedUrl("/login?suspended"));
        mockMvc.perform(post("/mypage/withdraw").with(learner).with(csrf()))
                .andExpect(redirectedUrl("/login?suspended"));
        mockMvc.perform(post("/api/auth/verification-mail").with(learner).with(csrf()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    void 로그인_유지_쿠키로도_정지_회원은_들어올_수_없다() throws Exception {
        Cookie rememberMe = mockMvc.perform(post("/login").with(csrf())
                        .param("email", LEARNER).param("password", PASSWORD).param("rememberMe", "true"))
                .andReturn().getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();
        mockMvc.perform(get("/mypage").cookie(rememberMe)).andExpect(status().isOk());

        suspend("DAYS_7", "쿠키", LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        mockMvc.perform(get("/mypage").cookie(rememberMe)).andExpect(status().is3xxRedirection());
    }

    @Test
    void 해제하면_다시_로그인할_수_있다() throws Exception {
        suspend("DAYS_30", "해제 확인", LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        mockMvc.perform(post("/admin/members/{id}/release", learnerId).with(as(ADMIN)).with(csrf()))
                .andExpect(flash().attribute("toast", "정지를 해제했습니다"));
        mockMvc.perform(post("/login").with(csrf()).param("email", LEARNER).param("password", PASSWORD))
                .andExpect(redirectedUrl("/courses"));
        mockMvc.perform(post("/admin/members/{id}/release", learnerId).with(as(ADMIN)).with(csrf()))
                .andExpect(flash().attribute("toast", "정지 중인 회원이 아닙니다."));
    }

    @Test
    void 운영자와_이미_정지된_회원은_정지할_수_없고_사유는_필수다() throws Exception {
        mockMvc.perform(post("/admin/members/{id}/suspend", adminId).with(as(ADMIN)).with(csrf())
                        .param("level", "DAYS_7").param("reason", "운영자"))
                .andExpect(flash().attribute("toast", "운영자 계정은 정지할 수 없습니다."));
        mockMvc.perform(post("/admin/members/{id}/suspend", learnerId).with(as(ADMIN)).with(csrf())
                        .param("level", "DAYS_7").param("reason", " "))
                .andExpect(flash().attribute("toast", "제재 단계와 사유를 입력해 주세요"));

        suspend("DAYS_7", "먼저", LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        mockMvc.perform(post("/admin/members/{id}/suspend", learnerId).with(as(ADMIN)).with(csrf())
                        .param("level", "DAYS_30").param("reason", "또"))
                .andExpect(flash().attribute("toast", "이미 이용 정지 중인 회원입니다."));
    }

    @Test
    void 정지_중에는_탈퇴할_수_없다() {
        suspend("DAYS_7", "탈퇴 막기", LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        assertThatThrownBy(() -> mypageService.withdraw(learnerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(AuthErrorCode.SUSPENDED_CANNOT_WITHDRAW);
    }

    @Test
    void 숨길_회원은_해제되지_않은_영구_정지_회원뿐이다() {
        suspend("PERMANENT", "영구", LocalDateTime.now(), null);

        assertThat(memberReader.findHiddenMemberIds(List.of(learnerId, adminId))).containsExactly(learnerId);
        jdbcTemplate.update("UPDATE member_suspension SET released_at = NOW() WHERE member_id = ?", learnerId);
        assertThat(memberReader.findHiddenMemberIds(List.of(learnerId))).isEmpty();
    }

    private void suspend(String level, String reason, LocalDateTime startsAt, LocalDateTime endsAt) {
        jdbcTemplate.update("INSERT INTO member_suspension (member_id, admin_member_id, level, reason, starts_at, ends_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)", learnerId, adminId, level, reason, startsAt, endsAt);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
