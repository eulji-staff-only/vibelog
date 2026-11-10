package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.entity.SocialProvider;
import com.vibelog.domain.auth.model.policy.SocialLoginSuccessHandler;
import com.vibelog.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/*
 * 구글·깃허브 로그인 (API-USR-07, PL-USR-003·005, #91).
 * 구글·깃허브 서버는 부르지 않고, 받은 값(고유 번호·이메일·이름)으로 회원을 찾거나 만드는 부분을 확인한다.
 * 키가 없는 기본 설정이라 로그인 화면에는 소셜 버튼이 없다 (키가 있을 때는 SocialLoginEnabledTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SocialLoginTest {

    @Autowired
    SocialMemberService socialMemberService;

    @Autowired
    SocialLoginSuccessHandler socialLoginSuccessHandler;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MockMvc mockMvc;

    @Test
    void 처음이면_인증된_회원과_소셜_연결을_만들고_다음에는_같은_회원이다() {
        LoginMember first = socialMemberService.login(SocialProvider.GITHUB, "gh-1001", "social-first@vibelog.test", "Vibe Coder");

        assertThat(first.isFirstLogin()).isTrue();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT nickname, password_hash, email_verified_at FROM member WHERE member_id = ?", first.getMemberId());
        assertThat(row.get("nickname")).isEqualTo("VibeCode");
        assertThat(row.get("password_hash")).isNull();
        assertThat(row.get("email_verified_at")).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT provider FROM social_account WHERE member_id = ?", String.class, first.getMemberId()))
                .isEqualTo("GITHUB");

        LoginMember again = socialMemberService.login(SocialProvider.GITHUB, "gh-1001", "social-first@vibelog.test", "Vibe Coder");
        assertThat(again.getMemberId()).isEqualTo(first.getMemberId());
        assertThat(again.isFirstLogin()).isFalse();
    }

    @Test
    void 같은_이메일이_다른_방식으로_가입돼_있으면_막는다() {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES ('taken@vibelog.test', 'x', '먼저가입')");

        assertThatThrownBy(() -> socialMemberService.login(SocialProvider.GOOGLE, "g-1", "taken@vibelog.test", "구글러"))
                .isInstanceOf(BusinessException.class).hasMessage("처음 가입한 방식(이메일·GitHub·구글)으로 로그인해 주세요.");

        socialMemberService.login(SocialProvider.GOOGLE, "g-2", "google-only@vibelog.test", "구글러");
        assertThatThrownBy(() -> socialMemberService.login(SocialProvider.GITHUB, "gh-2", "google-only@vibelog.test", "깃허버"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 닉네임은_규칙에_맞게_고치고_겹치거나_쓸_수_없으면_숫자를_붙인다() {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES ('nick@vibelog.test', 'x', '바이브')");
        jdbcTemplate.update("INSERT INTO banned_word (word) VALUES ('나쁜말')");

        assertThat(nicknameOf(socialMemberService.login(SocialProvider.GOOGLE, "g-3", "a@vibelog.test", "바이브")))
                .matches("바이브\\d{4}");
        assertThat(nicknameOf(socialMemberService.login(SocialProvider.GOOGLE, "g-4", "b@vibelog.test", "!")))
                .isEqualTo("회원");
        assertThat(nicknameOf(socialMemberService.login(SocialProvider.GOOGLE, "g-5", "c@vibelog.test", "나쁜말쟁이")))
                .matches("회원\\d{4}");
    }

    @Test
    void 처음_로그인하면_진단으로_가고_아니면_도착지_규칙을_따른다() throws Exception {
        LoginMember first = socialMemberService.login(SocialProvider.GOOGLE, "g-6", "dest@vibelog.test", "도착지");
        MockHttpServletResponse response = new MockHttpServletResponse();
        socialLoginSuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, new TestingAuthenticationToken(first, null));
        assertThat(response.getRedirectedUrl()).isEqualTo("/diagnosis");

        // 진단을 건너뛴 기록을 넣어 둔다. 진단 기록이 없으면 도착지 ③(#12)에 따라 /diagnosis 로 가기 때문이다
        jdbcTemplate.update("INSERT INTO diagnosis_result (member_id, recommended_track_id, status) VALUES (?, NULL, 'SKIPPED')", first.getMemberId());
        LoginMember again = socialMemberService.login(SocialProvider.GOOGLE, "g-6", "dest@vibelog.test", "도착지");
        response = new MockHttpServletResponse();
        socialLoginSuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, new TestingAuthenticationToken(again, null));
        assertThat(response.getRedirectedUrl()).isEqualTo("/courses");
    }

    @Test
    void 로그인_화면의_redirect_로_소셜_로그인_뒤_돌아가고_한_번_쓰면_지운다() throws Exception {
        LoginMember member = socialMemberService.login(SocialProvider.GITHUB, "gh-9", "back@vibelog.test", "돌아감");
        jdbcTemplate.update("INSERT INTO diagnosis_result (member_id, recommended_track_id, status) VALUES (?, NULL, 'SKIPPED')", member.getMemberId());
        LoginMember again = socialMemberService.login(SocialProvider.GITHUB, "gh-9", "back@vibelog.test", "돌아감");
        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/login").param("redirect", "/mypage"))
                .andReturn().getRequest().getSession();

        assertThat(socialRedirect(session, again)).isEqualTo("/mypage");
        assertThat(socialRedirect(session, again)).as("한 번 쓰면 지운다").isEqualTo("/courses");

        mockMvc.perform(get("/login").param("redirect", "/mypage").session(session));
        mockMvc.perform(get("/login").session(session));
        assertThat(socialRedirect(session, again)).as("redirect 없이 다시 열면 지난 값을 지운다").isEqualTo("/courses");
    }

    private String socialRedirect(MockHttpSession session, LoginMember member) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        socialLoginSuccessHandler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken(member, null));
        return response.getRedirectedUrl();
    }

    @Test
    void 소셜_가입_이메일로_폼_로그인하면_처음_가입한_방식을_안내한다() throws Exception {
        socialMemberService.login(SocialProvider.GITHUB, "gh-7", "form-social@vibelog.test", "폼로그인");

        mockMvc.perform(post("/login").with(csrf()).param("email", "form-social@vibelog.test").param("password", "anything12"))
                .andExpect(redirectedUrl("/login?social"));
        mockMvc.perform(get("/login").param("social", ""))
                .andExpect(content().string(containsString("처음 가입한 방식(이메일·GitHub·구글)으로 로그인해 주세요.")));
    }

    @Test
    void 키가_없으면_로그인_화면에_소셜_버튼이_없다() throws Exception {
        mockMvc.perform(get("/login")).andExpect(content().string(not(containsString("/oauth2/authorization/"))));
    }

    private String nicknameOf(LoginMember member) {
        return jdbcTemplate.queryForObject("SELECT nickname FROM member WHERE member_id = ?", String.class, member.getMemberId());
    }
}
