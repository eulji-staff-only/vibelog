package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import com.vibelog.domain.message.model.service.NotificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 사이트 안 알림 (API-NTF-01~03, FR-NTF-001, PL-NTF-002, #185) 과 댓글·답글 알림 연결 (계약 ⑬).
 * 회원·알림은 테스트마다 새로 넣고 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired NotificationService notificationService;
    @Autowired EntityManager entityManager;

    private String me;
    private long myId;

    @BeforeEach
    void 회원을_넣는다() {
        me = member();
        myId = idOf(me);
    }

    @Test
    void 목록은_최신순_20개씩이고_안_읽은_수와_다음_쪽_여부를_준다() throws Exception {
        LocalDateTime base = LocalDateTime.now().minusHours(1);
        for (int i = 0; i < 21; i++) {
            notify(myId, "RECOMMEND", "POST", 100 + i, "알림 " + i, base.plusMinutes(i));
        }

        mockMvc.perform(get("/api/notifications").with(as(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(21))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.items[0].content").value("알림 20"))
                .andExpect(jsonPath("$.items[0].notificationType").value("RECOMMEND"))
                .andExpect(jsonPath("$.items[0].read").value(false));
        mockMvc.perform(get("/api/notifications").param("page", "2").with(as(me)))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].content").value("알림 0"));
    }

    @Test
    void 알림을_열면_읽음을_남기고_종류별_도착지로_간다() throws Exception {
        long postId = postOf(myId);
        long commentId = commentOf(postId, myId);
        long recommend = notify(myId, "RECOMMEND", "POST", postId, "추천", LocalDateTime.now());
        long comment = notify(myId, "COMMENT", "COMMENT", commentId, "댓글", LocalDateTime.now());
        long message = notify(myId, "MESSAGE", "MESSAGE", 7, "쪽지", LocalDateTime.now());
        long badge = notify(myId, "BADGE", "CONTEST_AWARD", 8, "배지", LocalDateTime.now());
        long notice = notify(myId, "NOTICE", "NOTICE", 9, "공지", LocalDateTime.now());

        open(recommend).andExpect(redirectedUrl("/community/" + postId));
        open(comment).andExpect(redirectedUrl("/community/" + postId + "#comment-" + commentId));
        open(message).andExpect(redirectedUrl("/messages?box=received&messageId=7"));
        open(badge).andExpect(redirectedUrl("/mypage?tab=badges"));
        open(notice).andExpect(redirectedUrl("/notices/9"));

        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE member_id = ? AND read_at IS NULL",
                Long.class, myId)).isZero();
    }

    @Test
    void 남의_알림은_열_수_없고_없는_알림은_404() throws Exception {
        long others = notify(idOf(member()), "RECOMMEND", "POST", 1, "남의 알림", LocalDateTime.now());

        open(others).andExpect(status().isForbidden());
        mockMvc.perform(get("/notifications/{id}", others + 100_000).with(as(me))).andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("SELECT read_at FROM notification WHERE notification_id = ?",
                LocalDateTime.class, others)).isNull();
    }

    @Test
    void 모두_읽음을_누르면_내_알림만_모두_읽음이_된다() throws Exception {
        notify(myId, "RECOMMEND", "POST", 1, "하나", LocalDateTime.now());
        notify(myId, "RECOMMEND", "POST", 2, "둘", LocalDateTime.now());
        long otherId = idOf(member());
        notify(otherId, "RECOMMEND", "POST", 3, "남의 것", LocalDateTime.now());

        mockMvc.perform(post("/api/notifications/read-all").with(as(me)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(notificationService.countUnread(myId)).isZero();
        assertThat(notificationService.countUnread(otherId)).isEqualTo(1);
    }

    @Test
    void 헤더에_안_읽은_알림_수가_보인다() throws Exception {
        notify(myId, "RECOMMEND", "POST", 1, "하나", LocalDateTime.now());
        notify(myId, "RECOMMEND", "POST", 2, "둘", LocalDateTime.now());

        mockMvc.perform(get("/community").with(as(me)))
                .andExpect(content().string(containsString("알림, 안 읽은 알림 2개")));
    }

    @Test
    void 구십일_지난_알림만_지운다() {
        long old = notify(myId, "RECOMMEND", "POST", 1, "오래됨", LocalDateTime.now().minusDays(91));
        long recent = notify(myId, "RECOMMEND", "POST", 2, "아직", LocalDateTime.now().minusDays(89));

        notificationService.deleteOld();

        List<Long> left = jdbcTemplate.queryForList("SELECT notification_id FROM notification WHERE member_id = ?", Long.class, myId);
        assertThat(left).contains(recent).doesNotContain(old);
    }

    @Test
    void 댓글은_글쓴이에게_답글은_댓글쓴이에게_알리고_나에게는_알리지_않는다() throws Exception {
        long postId = postOf(myId);
        String commenter = member();
        String replier = member();

        mockMvc.perform(post("/community/{id}/comments", postId).param("content", "좋아요").with(as(commenter)).with(csrf()));
        entityManager.flush();
        long commentId = jdbcTemplate.queryForObject("SELECT MAX(comment_id) FROM comment WHERE post_id = ?", Long.class, postId);
        mockMvc.perform(post("/community/{id}/comments", postId).param("content", "답글")
                .param("parentCommentId", String.valueOf(commentId)).with(as(replier)).with(csrf()));
        mockMvc.perform(post("/community/{id}/comments", postId).param("content", "내 글에 내 댓글").with(as(me)).with(csrf()));
        entityManager.flush();

        assertThat(types(myId)).containsExactly("COMMENT");
        assertThat(types(idOf(commenter))).containsExactly("REPLY");
        assertThat(types(idOf(replier))).isEmpty();
    }

    private ResultActions open(long notificationId) throws Exception {
        return mockMvc.perform(get("/notifications/{id}", notificationId).with(as(me)));
    }

    private List<String> types(long memberId) {
        return jdbcTemplate.queryForList("SELECT notification_type FROM notification WHERE member_id = ?", String.class, memberId);
    }

    private long notify(long memberId, String type, String targetType, long targetId, String content, LocalDateTime createdAt) {
        jdbcTemplate.update("INSERT INTO notification (member_id, notification_type, target_type, target_id, content, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)", memberId, type, targetType, targetId, content, createdAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String member() {
        String email = "notify-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '알림회원', NOW())", email);
        return email;
    }

    private long idOf(String email) {
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, email);
    }

    private long postOf(long memberId) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body) VALUES (?, 'FREE', '알림 받을 글', '본문')", memberId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long commentOf(long postId, long memberId) {
        jdbcTemplate.update("INSERT INTO comment (post_id, member_id, content) VALUES (?, ?, '댓글')", postId, memberId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
