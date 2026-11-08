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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 게시글 즐겨찾기 (API-CMT-13, FR-CMT-007, #180)와 마이페이지 즐겨찾기 탭. 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FavoriteTest {

    private static final String EMAIL = "favorite-test@vibelog.test";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;

    private long postId;
    private long deletedPostId;

    @BeforeEach
    void 회원과_글을_넣는다() {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', '즐겨찾기회원')", EMAIL);
        long memberId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        postId = postOf(memberId, "즐겨찾기할 글", null);
        deletedPostId = postOf(memberId, "지운 즐겨찾기 글", LocalDateTime.now());
    }

    @Test
    void 같은_요청으로_즐겨찾기를_켜고_끈다() throws Exception {
        mockMvc.perform(post("/api/posts/{id}/favorite", postId).with(as(EMAIL)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.favorited").value(true));
        mockMvc.perform(get("/community/{id}", postId).with(as(EMAIL)))
                .andExpect(content().string(allOf(containsString("즐겨찾기 취소"), containsString("aria-pressed=\"true\""))));

        mockMvc.perform(post("/api/posts/{id}/favorite", postId).with(as(EMAIL)).with(csrf()))
                .andExpect(jsonPath("$.favorited").value(false));
        mockMvc.perform(get("/community/{id}", postId).with(as(EMAIL)))
                .andExpect(content().string(not(containsString("즐겨찾기 취소"))));
    }

    @Test
    void 없거나_지운_글은_찾을_수_없다() throws Exception {
        mockMvc.perform(post("/api/posts/{id}/favorite", deletedPostId).with(as(EMAIL)).with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        mockMvc.perform(post("/api/posts/{id}/favorite", 999_999_999L).with(as(EMAIL)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 비회원은_즐겨찾기_버튼이_없고_부를_수도_없다() throws Exception {
        mockMvc.perform(get("/community/{id}", postId)).andExpect(content().string(not(containsString("data-favorite"))));
        mockMvc.perform(post("/api/posts/{id}/favorite", postId).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void 마이페이지_즐겨찾기_탭에는_지운_글이_빠진다() throws Exception {
        long memberId = jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, EMAIL);
        jdbcTemplate.update("INSERT INTO favorite (member_id, post_id) VALUES (?, ?), (?, ?)", memberId, postId, memberId, deletedPostId);

        mockMvc.perform(get("/mypage").param("tab", "favorites").with(as(EMAIL)))
                .andExpect(content().string(allOf(containsString("즐겨찾기할 글"), not(containsString("지운 즐겨찾기 글")))));
    }

    @Test
    void 즐겨찾기한_글이_없으면_안내를_보여준다() throws Exception {
        mockMvc.perform(get("/mypage").param("tab", "favorites").with(as(EMAIL)))
                .andExpect(content().string(containsString("즐겨찾기한 글이 없습니다")));
    }

    private long postOf(long memberId, String title, LocalDateTime deletedAt) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body, deleted_at) VALUES (?, 'FREE', ?, '본문', ?)",
                memberId, title, deletedAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
