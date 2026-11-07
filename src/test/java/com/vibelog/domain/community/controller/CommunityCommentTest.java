package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 댓글·답글 쓰기, 댓글 수정·삭제 (API-CMT-08~10, #94). 넣은 회원·글·댓글은 테스트가 끝나면 되돌린다(@Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommunityCommentTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MemberLoginService memberLoginService;

    /* 테스트 전체가 한 트랜잭션이라, 변경 감지로 바뀐 값은 flush 해야 JdbcTemplate 으로 읽힌다 */
    @Autowired
    EntityManager entityManager;

    private String writer;
    private long postId;

    @BeforeEach
    void 인증한_회원과_글을_넣는다() {
        writer = member(true);
        postId = postOf(writer);
    }

    @Test
    void 댓글과_답글을_쓰면_그_댓글_위치로_돌아온다() throws Exception {
        comment(as(writer), postId, "첫 댓글", null)
                .andExpect(redirectedUrl("/community/" + postId + "#comment-" + lastCommentId()));
        long parentId = lastCommentId();

        comment(as(member(true)), postId, "답글입니다", parentId)
                .andExpect(redirectedUrl("/community/" + postId + "#comment-" + lastCommentId()));

        assertThat(jdbcTemplate.queryForObject("SELECT parent_comment_id FROM comment WHERE comment_id = ?", Long.class, lastCommentId()))
                .isEqualTo(parentId);
        mockMvc.perform(get("/community/" + postId))
                .andExpect(content().string(allOf(containsString("id=\"comment-" + parentId + "\""), containsString("답글입니다"))));
    }

    @Test
    void 답글에_답글을_달면_REPLY_DEPTH_EXCEEDED() throws Exception {
        long parentId = commentOf(writer, postId, null);
        long replyId = commentOf(writer, postId, parentId);

        comment(as(writer), postId, "답글의 답글", replyId)
                .andExpect(redirectedUrl("/community/" + postId + "#comment-form"))
                .andExpect(flash().attribute("commentError", "답글에는 답글을 달 수 없습니다."))
                .andExpect(flash().attribute("commentDraft", "답글의 답글"));
        assertThat(commentCount("답글의 답글")).isZero();
    }

    @Test
    void 다른_글의_댓글이나_삭제된_댓글에는_답글을_달_수_없다() throws Exception {
        long otherParent = commentOf(writer, postOf(writer), null);

        comment(as(writer), postId, "엉뚱한 답글", otherParent).andExpect(status().isNotFound());
        assertThat(commentCount("엉뚱한 답글")).isZero();

        // 삭제된 댓글에도 답글을 달 수 없다
        long deletedParent = commentOf(writer, postId, null);
        jdbcTemplate.update("UPDATE comment SET deleted_at = NOW() WHERE comment_id = ?", deletedParent);
        comment(as(writer), postId, "삭제된 댓글의 답글", deletedParent).andExpect(status().isNotFound());
        assertThat(commentCount("삭제된 댓글의 답글")).isZero();
    }

    @Test
    void 빈_댓글이나_500자가_넘는_댓글은_쓰던_내용과_함께_돌려보낸다() throws Exception {
        comment(as(writer), postId, "가".repeat(501), null)
                .andExpect(redirectedUrl("/community/" + postId + "#comment-form"))
                .andExpect(flash().attribute("commentError", "댓글은 500자까지 쓸 수 있습니다"))
                .andExpect(flash().attribute("commentDraft", "가".repeat(501)));
        comment(as(writer), postId, " ", null)
                .andExpect(flash().attribute("commentError", "댓글을 입력해 주세요"));
        comment(as(writer), postId, "나".repeat(500), null)
                .andExpect(redirectedUrl("/community/" + postId + "#comment-" + lastCommentId()));

        assertThat(commentCount("가".repeat(501))).isZero();
        assertThat(commentCount("나".repeat(500))).isOne();
    }

    @Test
    void 미인증_회원과_금칙어는_막는다() throws Exception {
        comment(as(member(false)), postId, "미인증 댓글", null)
                .andExpect(flash().attribute("commentError", "이메일 인증을 마치면 글을 쓸 수 있습니다."));
        jdbcTemplate.update("INSERT INTO banned_word (word) VALUES ('댓글금칙어')");
        comment(as(writer), postId, "여기 댓글금칙어 있음", null)
                .andExpect(flash().attribute("commentError", "사용할 수 없는 단어가 들어 있습니다."));

        assertThat(commentCount("미인증 댓글") + commentCount("여기 댓글금칙어 있음")).isZero();
    }

    @Test
    void 작성자는_수정하고_삭제하면_삭제된_댓글입니다가_보인다() throws Exception {
        long commentId = commentOf(writer, postId, null);

        mockMvc.perform(post("/comments/" + commentId + "/edit").param("content", "고친 댓글").with(as(writer)).with(csrf()))
                .andExpect(redirectedUrl("/community/" + postId + "#comment-" + commentId));
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT content FROM comment WHERE comment_id = ?", String.class, commentId))
                .isEqualTo("고친 댓글");

        mockMvc.perform(post("/comments/" + commentId + "/delete").with(as(writer)).with(csrf()))
                .andExpect(redirectedUrl("/community/" + postId));
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at IS NOT NULL FROM comment WHERE comment_id = ?", Boolean.class, commentId))
                .isTrue();
        mockMvc.perform(get("/community/" + postId))
                .andExpect(content().string(allOf(containsString("삭제된 댓글입니다"), not(containsString("고친 댓글")))));
    }

    @Test
    void 남의_댓글은_수정하거나_삭제할_수_없다() throws Exception {
        long commentId = commentOf(writer, postId, null);
        RequestPostProcessor other = as(member(true));

        mockMvc.perform(post("/comments/" + commentId + "/edit").param("content", "남이 고침").with(other).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/comments/" + commentId + "/delete").with(other).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForMap("SELECT content, deleted_at FROM comment WHERE comment_id = ?", commentId))
                .containsEntry("content", "댓글").containsEntry("deleted_at", null);
    }

    @Test
    void 비회원은_로그인_안내_미인증은_인증_안내_인증한_회원은_입력란이_보인다() throws Exception {
        long commentId = commentOf(writer, postId, null);

        mockMvc.perform(get("/community/" + postId))
                .andExpect(content().string(allOf(containsString("댓글은 로그인 후 남길 수 있습니다"),
                        not(containsString("id=\"comment-form\"")), not(containsString("/comments/" + commentId + "/edit")))));
        mockMvc.perform(get("/community/" + postId).with(as(member(false))))
                .andExpect(content().string(allOf(containsString("이메일 인증 후 댓글을 남길 수 있습니다"),
                        not(containsString("id=\"comment-form\"")))));
        mockMvc.perform(get("/community/" + postId).with(as(writer)))
                .andExpect(content().string(allOf(containsString("id=\"comment-form\""), containsString(">답글</summary>"),
                        containsString("/comments/" + commentId + "/edit"))));
    }

    @Test
    void 돌아온_입력_오류는_그_입력란을_열고_쓰던_내용을_채운다() throws Exception {
        long commentId = commentOf(writer, postId, null);

        mockMvc.perform(get("/community/" + postId).with(as(writer))
                        .flashAttr("commentTarget", "reply-" + commentId)
                        .flashAttr("commentError", "사용할 수 없는 단어가 들어 있습니다.")
                        .flashAttr("commentDraft", "쓰던 답글"))
                .andExpect(content().string(allOf(containsString("<details class=\"cm-inline\" open=\"open\">"),
                        containsString("사용할 수 없는 단어가 들어 있습니다."), containsString(">쓰던 답글</textarea>"))));
    }

    private ResultActions comment(RequestPostProcessor member, long postId, String content, Long parentCommentId) throws Exception {
        var request = post("/community/" + postId + "/comments").param("content", content);
        if (parentCommentId != null) {
            request.param("parentCommentId", String.valueOf(parentCommentId));
        }
        return mockMvc.perform(request.with(member).with(csrf()));
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }

    private String member(boolean emailVerified) {
        String email = "commenter-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '댓글러', ?)",
                email, emailVerified ? LocalDateTime.now() : null);
        return email;
    }

    private long postOf(String email) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body) "
                + "SELECT member_id, 'FREE', '댓글 달 글', '본문' FROM member WHERE email = ?", email);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long commentOf(String email, long postId, Long parentCommentId) {
        jdbcTemplate.update("INSERT INTO comment (post_id, member_id, parent_comment_id, content) "
                + "SELECT ?, member_id, ?, '댓글' FROM member WHERE email = ?", postId, parentCommentId, email);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /* 저장은 JPA 라 flush 한 뒤 가장 최근 댓글 ID 를 읽는다 */
    private long lastCommentId() {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT MAX(comment_id) FROM comment", Long.class);
    }

    private int commentCount(String content) {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comment WHERE content = ?", Integer.class, content);
    }
}
