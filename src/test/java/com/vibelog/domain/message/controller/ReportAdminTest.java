package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import jakarta.persistence.EntityManager;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/*
 * 운영자 신고 처리 A-13 (API-RPT-02~04, FR-RPT-004, PL-RPT-004, #257). 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportAdminTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired EntityManager entityManager;

    private final LocalDateTime now = LocalDateTime.now();
    private long admin;
    private long author;

    @BeforeEach
    void setUp() {
        admin = member("ADMIN");
        author = member("LEARNER");
    }

    @Test
    void 처리_대기_신고를_대상별로_묶어_보여주고_열면_원문과_신고_목록이_보인다() throws Exception {
        long postId = writePost("신고된 글 제목", "신고된 글 본문");
        long first = report("POST", postId, "SPAM", now.minusHours(3));
        report("POST", postId, "SPAM", now.minusHours(2));
        report("POST", postId, "ABUSE", now.minusHours(1));

        mockMvc.perform(get("/admin/reports").with(as(admin)))
                .andExpect(content().string(allOf(containsString("신고된 글 제목"), containsString("스팸 2, 욕설/비방 1"),
                        containsString("3건"), containsString("자동 숨김"))));
        mockMvc.perform(get("/admin/reports").param("reportId", String.valueOf(first)).with(as(admin)))
                .andExpect(content().string(allOf(containsString("신고된 글 본문"), containsString("게시글 원문"),
                        containsString("/admin/reports/" + first + "/hide"))));
    }

    @Test
    void 숨기면_대기_신고_전부가_숨김이_되고_작성자에게_가장_많은_사유로_알린다() throws Exception {
        long postId = writePost("광고 글", "본문");
        long first = report("POST", postId, "ABUSE", now.minusHours(3));
        report("POST", postId, "SPAM", now.minusHours(2));
        report("POST", postId, "SPAM", now.minusHours(1));

        mockMvc.perform(post("/admin/reports/{id}/hide", first).with(as(admin)).with(csrf()))
                .andExpect(redirectedUrl("/admin/reports")).andExpect(flash().attribute("toast", "숨김 처리했습니다"));
        entityManager.flush(); // 테스트는 한 트랜잭션이라, 상태 변경을 DB 에 내려 보낸 뒤 JdbcTemplate 으로 읽는다

        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT status FROM report WHERE target_type = 'POST' AND target_id = ?",
                String.class, postId)).containsExactly("HIDDEN");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM report WHERE target_id = ? AND handler_member_id = ? AND handled_at IS NOT NULL",
                Long.class, postId, admin)).isEqualTo(3);
        Map<String, Object> notification = jdbcTemplate.queryForMap("SELECT notification_type, target_type, content FROM notification"
                + " WHERE member_id = ? AND target_id = ?", author, postId);
        assertThat(notification.get("notification_type")).isEqualTo("HIDDEN");
        assertThat(notification.get("content")).isEqualTo("운영 정책에 따라 내 게시글 「광고 글」이(가) 숨겨졌습니다. 사유: 스팸");

        // 이미 처리된 신고는 다시 처리하지 못한다 (PL-RPT-004)
        mockMvc.perform(post("/admin/reports/{id}/dismiss", first).with(as(admin)).with(csrf()))
                .andExpect(redirectedUrl("/admin/reports")).andExpect(flash().attribute("toast", "이미 처리된 신고입니다."));
    }

    @Test
    void 기각하면_알림_없이_기각되고_목록에서_빠진다() throws Exception {
        long postId = writePost("기각할 글", "본문");
        long reportId = report("POST", postId, "SPAM", now.minusHours(1));

        mockMvc.perform(post("/admin/reports/{id}/dismiss", reportId).with(as(admin)).with(csrf()))
                .andExpect(flash().attribute("toast", "기각했습니다"));
        entityManager.flush();

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM report WHERE report_id = ?", String.class, reportId)).isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE member_id = ?", Long.class, author)).isZero();
        mockMvc.perform(get("/admin/reports").with(as(admin))).andExpect(content().string(not(containsString("기각할 글"))));
    }

    @Test
    void 없는_신고는_목록으로_돌아가고_숨긴_쪽지_알림은_보낸_쪽지함으로_간다() throws Exception {
        mockMvc.perform(get("/admin/reports").param("reportId", "-1").with(as(admin)))
                .andExpect(redirectedUrl("/admin/reports")).andExpect(flash().attribute("toast", "신고를 찾을 수 없습니다."));

        long receiver = member("LEARNER");
        long messageId = insert("INSERT INTO message (sender_member_id, receiver_member_id, content) VALUES (?, ?, '신고된 쪽지')", author, receiver);
        long reportId = insert("INSERT INTO report (reporter_member_id, target_type, target_id, target_member_id, reason, status)"
                + " VALUES (?, 'MESSAGE', ?, ?, 'ABUSE', 'PENDING')", receiver, messageId, author);
        mockMvc.perform(post("/admin/reports/{id}/hide", reportId).with(as(admin)).with(csrf()));

        long notificationId = jdbcTemplate.queryForObject("SELECT notification_id FROM notification WHERE member_id = ? AND notification_type = 'HIDDEN'",
                Long.class, author);
        mockMvc.perform(get("/notifications/{id}", notificationId).with(as(author)))
                .andExpect(redirectedUrl("/messages?box=sent&messageId=" + messageId));
    }

    @Test
    void 회원_상세에_받은_신고_이력이_보인다() throws Exception {
        long postId = writePost("이력 글", "본문");
        report("POST", postId, "SPAM", now.minusHours(1));
        insert("INSERT INTO report (reporter_member_id, target_type, target_id, target_member_id, reason, status, handled_at)"
                + " VALUES (?, 'COMMENT', 900001, ?, 'SPAM', 'REJECTED', NOW())", member("LEARNER"), author);

        mockMvc.perform(get("/admin/members/{id}", author).with(as(admin)))
                .andExpect(content().string(containsString("대기 1 · 숨김 0 · 기각 1")));
    }

    private long writePost(String title, String body) {
        return insert("INSERT INTO post (member_id, category, title, body) VALUES (?, 'FREE', ?, ?)", author, title, body);
    }

    /* 새 신고자 한 명이 author 의 대상을 신고한 행 */
    private long report(String targetType, long targetId, String reason, LocalDateTime createdAt) {
        return insert("INSERT INTO report (reporter_member_id, target_type, target_id, target_member_id, reason, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, 'PENDING', ?)", member("LEARNER"), targetType, targetId, author, reason, createdAt);
    }

    private long insert(String sql, Object... args) {
        jdbcTemplate.update(sql, args);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long member(String role) {
        return insert("INSERT INTO member (email, password_hash, nickname, role, email_verified_at) VALUES (?, 'x', '신고처리회원', ?, NOW())",
                "report-admin-" + UUID.randomUUID() + "@vibelog.test", role);
    }

    private RequestPostProcessor as(long memberId) {
        String email = jdbcTemplate.queryForObject("SELECT email FROM member WHERE member_id = ?", String.class, memberId);
        return user(memberLoginService.loadUserByUsername(email));
    }
}
