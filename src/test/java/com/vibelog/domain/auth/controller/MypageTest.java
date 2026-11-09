package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 마이페이지·비밀번호 변경·회원 탈퇴 (API-USR-15~17, #116). 넣은 회원·글·댓글은 테스트가 끝나면 되돌린다(@Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MypageTest {

    private static final String EMAIL = "mypage-test@vibelog.test";
    private static final String SOCIAL_EMAIL = "mypage-social@vibelog.test";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MemberLoginService memberLoginService;

    private long memberId;

    @BeforeEach
    void 이메일_회원과_소셜_회원을_넣는다() {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, ?, '마이페이지', NOW())",
                EMAIL, passwordEncoder.encode("oldpass1234"));
        memberId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, NULL, '소셜회원', NOW())", SOCIAL_EMAIL);
        jdbcTemplate.update("INSERT INTO social_account (member_id, provider, provider_user_id) SELECT member_id, 'GOOGLE', 'g-mypage' FROM member WHERE email = ?",
                SOCIAL_EMAIL);
    }

    @Test
    void 내_정보에_이메일_닉네임_가입_방식이_보인다() throws Exception {
        mockMvc.perform(get("/mypage").with(as(EMAIL)))
                .andExpect(content().string(allOf(containsString(EMAIL), containsString("마이페이지"),
                        containsString("<dd>이메일</dd>"), containsString("action=\"/mypage/password\""))));
        mockMvc.perform(get("/mypage").with(as(SOCIAL_EMAIL)))
                .andExpect(content().string(allOf(containsString("<dd>구글</dd>"), containsString("구글·깃허브로 가입한 계정입니다"),
                        not(containsString("action=\"/mypage/password\"")))));
    }

    @Test
    void 닉네임을_바꾸면_토스트와_함께_내_정보로_돌아온다() throws Exception {
        mockMvc.perform(post("/mypage/profile").with(as(EMAIL)).with(csrf()).param("nickname", "새닉네임1"))
                .andExpect(redirectedUrl("/mypage?tab=info"))
                .andExpect(flash().attribute("toast", "닉네임을 저장했습니다"));

        assertThat(nicknameOf(EMAIL)).isEqualTo("새닉네임1");
    }

    @Test
    void 닉네임이_규칙에_맞지_않거나_다른_회원이_쓰고_있으면_같은_화면에_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/mypage/profile").with(as(EMAIL)).with(csrf()).param("nickname", "탈퇴한 사용자"))
                .andExpect(view().name("auth/mypage"))
                .andExpect(model().attributeHasFieldErrors("nicknameChangeRequest", "nickname"));
        mockMvc.perform(post("/mypage/profile").with(as(EMAIL)).with(csrf()).param("nickname", "소셜회원"))
                .andExpect(model().attributeHasFieldErrorCode("nicknameChangeRequest", "nickname", "DUPLICATE_NICKNAME"))
                .andExpect(content().string(allOf(containsString("이미 사용 중인 닉네임입니다."), containsString("action=\"/mypage/password\""))));

        assertThat(nicknameOf(EMAIL)).isEqualTo("마이페이지");
    }

    @Test
    void 금칙어가_든_닉네임은_같은_화면에_메시지를_보여준다() throws Exception {
        jdbcTemplate.update("INSERT INTO banned_word (word) VALUES (?)", "닉금칙");

        mockMvc.perform(post("/mypage/profile").with(as(EMAIL)).with(csrf()).param("nickname", "나는닉금칙"))
                .andExpect(view().name("auth/mypage"))
                .andExpect(model().attributeHasFieldErrorCode("nicknameChangeRequest", "nickname", "BANNED_WORD"));

        assertThat(nicknameOf(EMAIL)).isEqualTo("마이페이지");
    }

    @Test
    void 내_닉네임의_대소문자만_바꿔도_저장된다() throws Exception {
        jdbcTemplate.update("UPDATE member SET nickname = 'Alice' WHERE email = ?", EMAIL);

        mockMvc.perform(post("/mypage/profile").with(as(EMAIL)).with(csrf()).param("nickname", "alice"))
                .andExpect(redirectedUrl("/mypage?tab=info"));

        assertThat(nicknameOf(EMAIL)).isEqualTo("alice");
    }

    @Test
    void 지금_닉네임_그대로_저장해도_된다() throws Exception {
        mockMvc.perform(post("/mypage/profile").with(as(EMAIL)).with(csrf()).param("nickname", "마이페이지"))
                .andExpect(redirectedUrl("/mypage?tab=info"));
    }

    @Test
    void 비밀번호를_바꾸면_토스트와_함께_내_정보로_돌아온다() throws Exception {
        mockMvc.perform(post("/mypage/password").with(as(EMAIL)).with(csrf())
                        .param("currentPassword", "oldpass1234").param("newPassword", "newpass5678"))
                .andExpect(redirectedUrl("/mypage?tab=info"))
                .andExpect(flash().attribute("toast", "비밀번호를 변경했습니다"));

        assertThat(passwordEncoder.matches("newpass5678", passwordHash(EMAIL))).isTrue();
    }

    @Test
    void 규칙에_맞지_않거나_현재_비밀번호가_틀리거나_소셜_계정이면_같은_화면에_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/mypage/password").with(as(EMAIL)).with(csrf())
                        .param("currentPassword", "oldpass1234").param("newPassword", "short"))
                .andExpect(view().name("auth/mypage"))
                .andExpect(model().attributeHasFieldErrors("passwordChangeRequest", "newPassword"));
        mockMvc.perform(post("/mypage/password").with(as(EMAIL)).with(csrf())
                        .param("currentPassword", "wrongpass99").param("newPassword", "newpass5678"))
                .andExpect(model().attributeHasFieldErrorCode("passwordChangeRequest", "currentPassword", "PASSWORD_MISMATCH"))
                .andExpect(content().string(containsString("현재 비밀번호가 맞지 않습니다.")));
        mockMvc.perform(post("/mypage/password").with(as(SOCIAL_EMAIL)).with(csrf())
                        .param("currentPassword", "anything12").param("newPassword", "newpass5678"))
                .andExpect(model().attributeHasFieldErrorCode("passwordChangeRequest", "newPassword", "SOCIAL_ACCOUNT"));

        assertThat(passwordEncoder.matches("oldpass1234", passwordHash(EMAIL))).isTrue();
        assertThat(passwordHash(SOCIAL_EMAIL)).isNull();
    }

    @Test
    void 작성한_글과_댓글은_지운_것을_빼고_보인다() throws Exception {
        long post = postOf(memberId, "내가 쓴 글", false);
        long deletedPost = postOf(memberId, "지운 글", true);
        commentOf(memberId, post, "남아 있는 댓글", false);
        commentOf(memberId, post, "지운 댓글", true);
        commentOf(memberId, deletedPost, "지운 글의 댓글", false);
        long live = jdbcTemplate.queryForObject("SELECT comment_id FROM comment WHERE content = '남아 있는 댓글'", Long.class);

        mockMvc.perform(get("/mypage").param("tab", "posts").with(as(EMAIL)))
                .andExpect(content().string(allOf(containsString("내가 쓴 글"), containsString("댓글 1"), not(containsString("지운 글")))));
        mockMvc.perform(get("/mypage").param("tab", "comments").with(as(EMAIL)))
                .andExpect(content().string(allOf(containsString("남아 있는 댓글"), containsString("/community/" + post + "#comment-" + live),
                        not(containsString("지운 댓글")), not(containsString("지운 글의 댓글")))));
        mockMvc.perform(get("/mypage").param("tab", "comments").with(as(SOCIAL_EMAIL)))
                .andExpect(content().string(containsString("작성한 댓글이 없습니다")));
    }

    @Test
    void 탈퇴하면_계정_정보를_비우고_로그아웃하고_글은_탈퇴한_사용자로_남는다() throws Exception {
        long post = postOf(memberId, "탈퇴 전에 쓴 글", false);
        var staleSession = memberLoginService.loadUserByUsername(EMAIL);
        jdbcTemplate.update("INSERT INTO email_verification (email, token_hash, purpose, expires_at) VALUES (?, 'hash-mypage', 'SIGNUP', NOW())", EMAIL);

        mockMvc.perform(post("/mypage/withdraw").with(as(EMAIL)).with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("toast", "회원 탈퇴가 완료되었습니다"))
                .andExpect(cookie().maxAge("remember-me", 0));
        entityManager.flush();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, email, password_hash, nickname, withdrawn_at FROM member WHERE member_id = ?", memberId);
        assertThat(row).containsEntry("status", "WITHDRAWN").containsEntry("email", null).containsEntry("password_hash", null)
                .containsEntry("nickname", "탈퇴한 사용자");
        assertThat(row.get("withdrawn_at")).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM email_verification WHERE email = ?", Integer.class, EMAIL)).isZero();
        mockMvc.perform(get("/community/" + post)).andExpect(content().string(containsString("탈퇴한 사용자")));

        // 다른 기기에 남은 세션(탈퇴 전 로그인 정보)으로는 마이페이지·다시 탈퇴를 쓸 수 없다
        mockMvc.perform(get("/mypage").with(user(staleSession))).andExpect(status().isNotFound());
        mockMvc.perform(post("/mypage/withdraw").with(user(staleSession)).with(csrf())).andExpect(status().isNotFound());

        // 같은 이메일로 다시 가입할 수 있다
        mockMvc.perform(post("/signup").with(csrf()).param("nickname", "다시가입").param("email", EMAIL).param("password", "again1234"))
                .andExpect(redirectedUrl("/signup?done=true"));
    }

    @Test
    void 소셜_회원이_탈퇴하면_소셜_연결도_지운다() throws Exception {
        mockMvc.perform(post("/mypage/withdraw").with(as(SOCIAL_EMAIL)).with(csrf())).andExpect(redirectedUrl("/"));
        entityManager.flush();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM social_account WHERE provider_user_id = 'g-mypage'", Integer.class)).isZero();
    }

    @Test
    void 메인에_탈퇴_완료_알림이_보인다() throws Exception {
        mockMvc.perform(get("/").flashAttr("toast", "회원 탈퇴가 완료되었습니다"))
                .andExpect(content().string(containsString("회원 탈퇴가 완료되었습니다")));
    }

    @Test
    void 북마크_탭에_북마크한_개념이_최근_순으로_보이고_누르면_수업_화면으로_간다() throws Exception {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9601, '트랙', '대상', '설명', 9601)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9601, '카테고리', 9601)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9601, 9601, 9601, '북마크 코스', '요약', '설명', '대상', '목표', 7, TRUE)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9601, 9601, '북마크 수업', '소개', '목표', '본문', 30, 1, TRUE)");
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9601, 9601, 'CONCEPT', '먼저 담은 개념', '본문', 'MARKDOWN', 10, NULL, 1), (9602, 9601, 'CONCEPT', '나중에 담은 개념', '본문', 'MARKDOWN', 10, NULL, 2)");
        jdbcTemplate.update("INSERT INTO concept_bookmark (member_id, activity_id, created_at) VALUES (?, 9601, NOW() - INTERVAL 1 DAY), (?, 9602, NOW())",
                memberId, memberId);

        String html = mockMvc.perform(get("/mypage").param("tab", "bookmarks").with(as(EMAIL)))
                .andExpect(content().string(allOf(containsString("북마크 코스 · 북마크 수업"),
                        containsString("href=\"/courses/9601/lessons/9601?activity=9602\""))))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("나중에 담은 개념")).isLessThan(html.indexOf("먼저 담은 개념"));

        mockMvc.perform(get("/mypage").param("tab", "bookmarks").with(as(SOCIAL_EMAIL)))
                .andExpect(content().string(containsString("북마크한 개념이 없습니다")));
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }

    private long postOf(long memberId, String title, boolean deleted) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body, deleted_at) VALUES (?, 'FREE', ?, '본문', ?)",
                memberId, title, deleted ? java.time.LocalDateTime.now() : null);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String nicknameOf(String email) {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT nickname FROM member WHERE email = ?", String.class, email);
    }

    private void commentOf(long memberId, long postId, String content, boolean deleted) {
        jdbcTemplate.update("INSERT INTO comment (post_id, member_id, content, deleted_at) VALUES (?, ?, ?, ?)",
                postId, memberId, content, deleted ? java.time.LocalDateTime.now() : null);
    }

    private String passwordHash(String email) {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT password_hash FROM member WHERE email = ?", String.class, email);
    }
}
