package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 신고하기 (API-RPT-01, M-11, FR-RPT-001, PL-RPT-001·003, #255). 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;

    private String author;
    private String reporter;

    @BeforeEach
    void setUp() {
        author = member(true);
        reporter = member(true);
    }

    @Test
    void 게시글을_신고하면_처리_대기로_저장되고_같은_대상은_두_번_못_한다() throws Exception {
        long postId = writePost(author, false);

        report(reporter, "POST", postId, "SPAM", null).andExpect(status().isNoContent());

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT status, target_member_id, reason, reason_detail FROM report"
                + " WHERE target_type = 'POST' AND target_id = ?", postId);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(((Number) row.get("target_member_id")).longValue()).isEqualTo(idOf(author));
        assertThat(row.get("reason")).isEqualTo("SPAM");
        assertThat(row.get("reason_detail")).isNull();
        report(reporter, "POST", postId, "ABUSE", null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_REPORTED"));
    }

    @Test
    void 볼_수_없는_대상과_내_콘텐츠와_미인증_회원은_막는다() throws Exception {
        long postId = writePost(author, false);
        long deleted = writePost(author, true);
        long hiddenByAdmin = writePost(author, false);
        jdbcTemplate.update("INSERT INTO report (reporter_member_id, target_type, target_id, target_member_id, reason, status, handled_at)"
                + " VALUES (?, 'POST', ?, ?, 'SPAM', 'HIDDEN', NOW())", idOf(member(true)), hiddenByAdmin, idOf(author));

        report(author, "POST", postId, "SPAM", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SELF_REPORT"));
        report(reporter, "POST", deleted, "SPAM", null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));
        report(reporter, "POST", -1L, "SPAM", null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));
        report(reporter, "POST", hiddenByAdmin, "SPAM", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));
        report(member(false), "POST", postId, "SPAM", null).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void 기타는_내용을_써야_하고_잘못된_값은_INVALID_INPUT() throws Exception {
        long postId = writePost(author, false);

        report(reporter, "POST", postId, "ETC", "  ").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        report(reporter, "NOPE", postId, "SPAM", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        report(reporter, "POST", postId, null, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        report(reporter, "POST", postId, "ETC", "  광고 링크를 계속 올립니다 ").andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT reason_detail FROM report WHERE target_type = 'POST' AND target_id = ?",
                String.class, postId)).isEqualTo("광고 링크를 계속 올립니다");
    }

    @Test
    void 댓글_리뷰_공개_포트폴리오_받은_쪽지를_신고할_수_있다() throws Exception {
        long commentId = insert("INSERT INTO comment (post_id, member_id, content) VALUES (?, ?, '댓글')", writePost(author, false), idOf(author));
        long publicPortfolio = portfolio(author, true);
        long privatePortfolio = portfolio(author, false);
        long requestId = insert("INSERT INTO review_request (portfolio_id, requested_at) VALUES (?, NOW())", publicPortfolio);
        long reviewId = insert("INSERT INTO peer_review (review_request_id, reviewer_member_id) VALUES (?, ?)", requestId, idOf(author));
        insert("INSERT INTO review_comment (peer_review_id, content) VALUES (?, '리뷰 내용')", reviewId);
        long received = message(author, reporter);
        long sent = message(reporter, author);

        report(reporter, "COMMENT", commentId, "ABUSE", null).andExpect(status().isNoContent());
        report(reporter, "REVIEW", reviewId, "ABUSE", null).andExpect(status().isNoContent());
        report(reporter, "PORTFOLIO", publicPortfolio, "PLAGIARISM", null).andExpect(status().isNoContent());
        report(reporter, "MESSAGE", received, "PRIVACY", null).andExpect(status().isNoContent());
        report(reporter, "PORTFOLIO", privatePortfolio, "SPAM", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));
        report(reporter, "MESSAGE", sent, "SPAM", null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));

        assertThat(jdbcTemplate.queryForObject("SELECT target_member_id FROM report WHERE target_type = 'MESSAGE' AND target_id = ?",
                Long.class, received)).isEqualTo(idOf(author));
    }

    @Test
    void 영구_정지_회원의_글과_댓글_볼_수_없는_작품의_리뷰는_신고할_수_없다() throws Exception {
        long postId = writePost(author, false);
        long deletedPost = writePost(author, true);
        long privatePortfolio = portfolio(reporter, false);
        long requestId = insert("INSERT INTO review_request (portfolio_id, requested_at) VALUES (?, NOW())", privatePortfolio);
        long reviewOnMine = insert("INSERT INTO peer_review (review_request_id, reviewer_member_id) VALUES (?, ?)", requestId, idOf(author));
        insert("INSERT INTO review_comment (peer_review_id, content) VALUES (?, '리뷰 내용')", reviewOnMine);
        String stranger = member(true);
        String commenter = member(true);
        long commentOnPost = insert("INSERT INTO comment (post_id, member_id, content) VALUES (?, ?, '댓글')", postId, idOf(commenter));
        long commentOnDeleted = insert("INSERT INTO comment (post_id, member_id, content) VALUES (?, ?, '댓글')", deletedPost, idOf(commenter));

        // 지운 글의 댓글은 볼 수 없다
        report(stranger, "COMMENT", commentOnDeleted, "SPAM", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));

        // 비공개 작품의 리뷰: 남은 막고, 작품 주인은 받은 리뷰를 신고할 수 있다
        report(stranger, "REVIEW", reviewOnMine, "SPAM", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));
        report(reporter, "REVIEW", reviewOnMine, "SPAM", null).andExpect(status().isNoContent());

        insert("INSERT INTO member_suspension (member_id, admin_member_id, level, reason, starts_at) VALUES (?, ?, 'PERMANENT', '테스트', NOW() - INTERVAL 1 DAY)",
                idOf(author), idOf(member(true)));
        report(stranger, "POST", postId, "SPAM", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));
        // 글쓴이가 영구 정지면 그 글의 다른 회원 댓글도 볼 수 없다
        report(stranger, "COMMENT", commentOnPost, "SPAM", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_TARGET_NOT_FOUND"));
    }

    @Test
    void 남의_글과_받은_쪽지에만_신고_버튼이_보인다() throws Exception {
        long postId = writePost(author, false);
        long received = message(author, reporter);

        mockMvc.perform(get("/community/{id}", postId).with(as(reporter)))
                .andExpect(content().string(allOf(containsString("data-report-type=\"POST\""), containsString("id=\"report-modal\""))));
        mockMvc.perform(get("/community/{id}", postId).with(as(author)))
                .andExpect(content().string(not(containsString("data-report-type=\"POST\""))));
        mockMvc.perform(get("/messages").param("box", "received").param("messageId", String.valueOf(received)).with(as(reporter)))
                .andExpect(content().string(containsString("data-report-type=\"MESSAGE\"")));
    }

    private ResultActions report(String email, String targetType, long targetId, String reason, String detail) throws Exception {
        String json = "{\"targetType\":\"" + targetType + "\",\"targetId\":" + targetId
                + ",\"reason\":" + (reason == null ? "null" : "\"" + reason + "\"")
                + ",\"reasonDetail\":" + (detail == null ? "null" : "\"" + detail + "\"") + "}";
        return mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(json).with(as(email)).with(csrf()));
    }

    private long writePost(String email, boolean deleted) {
        return insert("INSERT INTO post (member_id, category, title, body, deleted_at) VALUES (?, 'FREE', '신고 테스트 글', '본문', "
                + (deleted ? "NOW()" : "NULL") + ")", idOf(email));
    }

    private long portfolio(String email, boolean isPublic) {
        return insert("INSERT INTO portfolio (member_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public)"
                + " VALUES (?, 'PERSONAL', '작품', 'https://example.com', '요약', '회고', 'Claude', ?)", idOf(email), isPublic);
    }

    private long message(String senderEmail, String receiverEmail) {
        return insert("INSERT INTO message (sender_member_id, receiver_member_id, content) VALUES (?, ?, '쪽지 내용')",
                idOf(senderEmail), idOf(receiverEmail));
    }

    private long insert(String sql, Object... args) {
        jdbcTemplate.update(sql, args);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String member(boolean verified) {
        String email = "report-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '신고회원', ?)",
                email, verified ? java.time.LocalDateTime.now() : null);
        return email;
    }

    private long idOf(String email) {
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, email);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
