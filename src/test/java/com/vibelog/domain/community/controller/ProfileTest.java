package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 공개 프로필·팔로우 (API-CMT-14·15, FR-CMT-008·009, #181)와 마이페이지 팔로우 탭, 게시글 작성자 링크.
 * 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileTest {

    private static final String ME = "profile-me@vibelog.test";
    private static final String WRITER = "profile-writer@vibelog.test";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;

    private long meId;
    private long writerId;
    private long withdrawnId;

    @BeforeEach
    void 회원과_글과_포트폴리오를_넣는다() {
        meId = memberOf(ME, "프로필보는사람");
        writerId = memberOf(WRITER, "프로필주인");
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, status) VALUES (NULL, NULL, '탈퇴한 사용자', 'WITHDRAWN')");
        withdrawnId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        postOf(writerId, "주인이 쓴 글", null);
        postOf(writerId, "주인이 지운 글", LocalDateTime.now());
        portfolioOf(writerId, "공개 작품", true, null);
        portfolioOf(writerId, "비공개 작품", false, null);
        portfolioOf(writerId, "지운 작품", true, LocalDateTime.now());
    }

    @Test
    void 공개_프로필에_닉네임_게시글_공개_포트폴리오가_보인다() throws Exception {
        mockMvc.perform(get("/users/{id}", writerId).with(as(ME)))
                .andExpect(content().string(allOf(containsString("프로필주인"), containsString("주인이 쓴 글"),
                        containsString("공개 작품"), not(containsString("주인이 지운 글")),
                        not(containsString("비공개 작품")), not(containsString("지운 작품")),
                        containsString("/api/users/" + writerId + "/follow"))));
    }

    @Test
    void 내_프로필에는_팔로우_버튼이_없다() throws Exception {
        mockMvc.perform(get("/users/{id}", writerId).with(as(WRITER)))
                .andExpect(content().string(not(containsString("/follow\""))));
    }

    @Test
    void 탈퇴했거나_없는_회원은_찾을_수_없고_비회원은_로그인으로() throws Exception {
        mockMvc.perform(get("/users/{id}", withdrawnId).with(as(ME))).andExpect(status().isNotFound());
        mockMvc.perform(get("/users/{id}", 999_999_999L).with(as(ME))).andExpect(status().isNotFound());
        mockMvc.perform(get("/users/{id}", writerId)).andExpect(redirectedUrl("/login?redirect=%2Fusers%2F" + writerId));
    }

    @Test
    void 같은_요청으로_팔로우를_켜고_끄고_팔로워_수를_준다() throws Exception {
        mockMvc.perform(post("/api/users/{id}/follow", writerId).with(as(ME)).with(csrf()))
                .andExpect(jsonPath("$.following").value(true)).andExpect(jsonPath("$.followerCount").value(1));
        mockMvc.perform(get("/mypage").param("tab", "following").with(as(ME)))
                .andExpect(content().string(containsString("프로필주인")));

        mockMvc.perform(post("/api/users/{id}/follow", writerId).with(as(ME)).with(csrf()))
                .andExpect(jsonPath("$.following").value(false)).andExpect(jsonPath("$.followerCount").value(0));
        mockMvc.perform(get("/mypage").param("tab", "following").with(as(ME)))
                .andExpect(content().string(containsString("팔로우한 회원이 없습니다")));
    }

    @Test
    void 팔로우_탭은_탈퇴한_회원을_뺀_뒤_쪽을_나눈다() throws Exception {
        // 먼저 팔로우한 활동 회원 1명 뒤에, 더 최근에 팔로우했다가 탈퇴한 회원 20명 (한 쪽 20명)
        jdbcTemplate.update("INSERT INTO follow (follower_member_id, following_member_id, created_at) VALUES (?, ?, ?)",
                meId, writerId, LocalDateTime.now().minusDays(1));
        for (int i = 0; i < 20; i++) {
            jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, status) VALUES (NULL, NULL, '탈퇴한 사용자', 'WITHDRAWN')");
            long gone = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            jdbcTemplate.update("INSERT INTO follow (follower_member_id, following_member_id) VALUES (?, ?)", meId, gone);
        }

        mockMvc.perform(get("/mypage").param("tab", "following").with(as(ME)))
                .andExpect(content().string(allOf(containsString("프로필주인"), not(containsString("1 / 2")))));
    }

    @Test
    void 나와_탈퇴한_회원은_팔로우할_수_없다() throws Exception {
        mockMvc.perform(post("/api/users/{id}/follow", meId).with(as(ME)).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SELF_FOLLOW"));
        mockMvc.perform(post("/api/users/{id}/follow", withdrawnId).with(as(ME)).with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void 게시글_작성자는_볼_수_있는_회원일_때만_프로필_링크가_걸린다() throws Exception {
        long postId = postOf(writerId, "링크 확인 글", null);
        long withdrawnPostId = postOf(withdrawnId, "탈퇴 회원 글", null);

        mockMvc.perform(get("/community/{id}", postId))
                .andExpect(content().string(containsString("href=\"/users/" + writerId + "\"")));
        mockMvc.perform(get("/community/{id}", withdrawnPostId))
                .andExpect(content().string(not(containsString("href=\"/users/" + withdrawnId + "\""))));
    }

    private long memberOf(String email, String nickname) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', ?)", email, nickname);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long postOf(long memberId, String title, LocalDateTime deletedAt) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body, deleted_at) VALUES (?, 'FREE', ?, '본문', ?)",
                memberId, title, deletedAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void portfolioOf(long memberId, String title, boolean isPublic, LocalDateTime deletedAt) {
        jdbcTemplate.update("INSERT INTO portfolio (member_id, course_id, portfolio_type, title, result_url, summary, retrospective,"
                        + " ai_tools, is_public, deleted_at) VALUES (?, NULL, 'PERSONAL', ?, 'https://example.com', '요약', '회고', 'Claude', ?, ?)",
                memberId, title, isPublic, deletedAt);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
