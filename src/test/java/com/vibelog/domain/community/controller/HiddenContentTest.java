package com.vibelog.domain.community.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 영구 정지 회원의 공개 콘텐츠 숨김 — 게시판 목록·상세·댓글 (PL-RPT-006, PL-ERR-006, #215).
 * 기간 정지와 해제된 영구 정지는 그대로 보인다. 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HiddenContentTest {

    private static final String HIDDEN_TEXT = "운영 정책에 따라 숨겨진 콘텐츠입니다";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    private long adminId;
    private long writerId;

    @BeforeEach
    void 운영자와_글쓴이를_넣는다() {
        adminId = member("ADMIN");
        writerId = member("LEARNER");
    }

    @Test
    void 영구_정지_회원의_글은_목록에서_빠지고_상세는_숨김_문구를_보여준다() throws Exception {
        String title = "숨길글" + UUID.randomUUID().toString().substring(0, 6);
        long postId = post(writerId, title);
        suspend(writerId, "PERMANENT", null, null);

        // 검색어 칸에 제목이 다시 보이므로 글 링크로 본다
        mockMvc.perform(get("/community").param("q", title)).andExpect(content().string(not(containsString(link(postId)))));
        mockMvc.perform(get("/community/{id}", postId))
                .andExpect(status().isForbidden()).andExpect(content().string(containsString(HIDDEN_TEXT)));
    }

    @Test
    void 기간_정지와_해제된_영구_정지는_그대로_보인다() throws Exception {
        long days = member("LEARNER");
        long released = member("LEARNER");
        String daysTitle = "기간정지글" + UUID.randomUUID().toString().substring(0, 6);
        String releasedTitle = "해제된글" + UUID.randomUUID().toString().substring(0, 6);
        long daysPost = post(days, daysTitle);
        long releasedPost = post(released, releasedTitle);
        suspend(days, "DAYS_7", LocalDateTime.now().plusDays(7), null);
        suspend(released, "PERMANENT", null, LocalDateTime.now());

        mockMvc.perform(get("/community").param("q", daysTitle)).andExpect(content().string(containsString(link(daysPost))));
        mockMvc.perform(get("/community").param("q", releasedTitle)).andExpect(content().string(containsString(link(releasedPost))));
    }

    @Test
    void 영구_정지_회원의_댓글은_자리만_남기고_내용을_숨긴다() throws Exception {
        long postId = post(writerId, "댓글 달린 글");
        long hiddenCommenter = member("LEARNER");
        jdbcTemplate.update("INSERT INTO comment (post_id, member_id, content) VALUES (?, ?, '숨겨야 할 댓글')", postId, hiddenCommenter);
        jdbcTemplate.update("INSERT INTO comment (post_id, member_id, content) VALUES (?, ?, '보이는 댓글')", postId, writerId);
        suspend(hiddenCommenter, "PERMANENT", null, null);

        mockMvc.perform(get("/community/{id}", postId))
                .andExpect(content().string(allOf(containsString(HIDDEN_TEXT), containsString("보이는 댓글"),
                        not(containsString("숨겨야 할 댓글")))));
    }

    private static String link(long postId) {
        return "href=\"/community/" + postId + "\"";
    }

    private void suspend(long memberId, String level, LocalDateTime endsAt, LocalDateTime releasedAt) {
        jdbcTemplate.update("INSERT INTO member_suspension (member_id, admin_member_id, level, reason, starts_at, ends_at, released_at)"
                + " VALUES (?, ?, ?, '사유', ?, ?, ?)", memberId, adminId, level, LocalDateTime.now().minusDays(1), endsAt, releasedAt);
    }

    private long post(long memberId, String title) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body) VALUES (?, 'FREE', ?, '본문')", memberId, title);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long member(String role) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, role, email_verified_at) VALUES (?, 'x', '숨김회원', ?, NOW())",
                "hide-" + UUID.randomUUID() + "@vibelog.test", role);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
