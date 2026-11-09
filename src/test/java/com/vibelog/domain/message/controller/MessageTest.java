package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import com.vibelog.domain.message.model.dto.MessageItem;
import com.vibelog.domain.message.model.service.MessageService;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 쪽지 (API-MSG-01~05, FR-MSG-001·002, PL-MSG-001·003, #184). 회원·쪽지는 테스트마다 새로 넣고 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MessageTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired MessageService messageService;
    @Autowired EntityManager entityManager;

    private String prefix;
    private String sender;
    private long senderId;
    private String receiver;
    private long receiverId;

    @BeforeEach
    void 보내는_사람과_받는_사람을_넣는다() {
        prefix = "m" + UUID.randomUUID().toString().substring(0, 5);
        sender = member(prefix + "보냄", true, "ACTIVE");
        senderId = idOf(sender);
        receiver = member(prefix + "받음", true, "ACTIVE");
        receiverId = idOf(receiver);
    }

    @Test
    void 쪽지를_보내면_보낸_쪽지함에서_열리고_받는_사람에게_알림이_간다() throws Exception {
        write(sender, receiverId, "안녕하세요").andExpect(flash().attribute("toast", "쪽지를 보냈습니다"));
        long messageId = lastMessageId();

        mockMvc.perform(post("/messages").with(as(sender)).with(csrf()).param("receiverMemberId", String.valueOf(receiverId))
                        .param("content", "두 번째"))
                .andExpect(redirectedUrl("/messages?box=sent&messageId=" + (lastMessageId())));
        assertThat(jdbcTemplate.queryForList("SELECT content FROM notification WHERE member_id = ? AND notification_type = 'MESSAGE' "
                + "AND target_id = ?", String.class, receiverId, messageId)).containsExactly(prefix + "보냄님이 쪽지를 보냈습니다");
        mockMvc.perform(get("/messages").param("box", "received").with(as(receiver)))
                .andExpect(content().string(allOf(containsString(prefix + "보냄"), containsString("안녕하세요"), containsString("is-unread"))));
    }

    @Test
    void 목록_미리보기는_줄바꿈과_탭을_한_칸으로_줄이고_40자에서_자른다() {
        assertThat(MessageItem.preview("첫 줄\n\n둘째\t줄  ")).isEqualTo("첫 줄 둘째 줄");
        assertThat(MessageItem.preview("가".repeat(41))).isEqualTo("가".repeat(40) + "…");
    }

    @Test
    void 받은_사람이_열면_읽음이_남고_주고받은_두_사람만_열_수_있다() throws Exception {
        long messageId = messageOf(senderId, receiverId, null, "비밀 쪽지", LocalDateTime.now());

        mockMvc.perform(get("/messages").param("messageId", String.valueOf(messageId)).with(as(sender)))
                .andExpect(content().string(containsString("비밀 쪽지")));
        assertThat(readAt(messageId)).isNull();
        mockMvc.perform(get("/messages").param("messageId", String.valueOf(messageId)).with(as(receiver)))
                .andExpect(content().string(allOf(containsString("답장 보내기"), not(containsString("is-unread")))));
        assertThat(readAt(messageId)).isNotNull();

        String stranger = member(prefix + "남", true, "ACTIVE");
        mockMvc.perform(get("/messages").param("messageId", String.valueOf(messageId)).with(as(stranger))).andExpect(status().isForbidden());
        mockMvc.perform(get("/messages").param("messageId", "999999999").with(as(receiver))).andExpect(status().isNotFound());
    }

    @Test
    void 답장은_원래_보낸_사람에게_가고_내가_받은_쪽지에만_할_수_있다() throws Exception {
        long messageId = messageOf(senderId, receiverId, null, "질문", LocalDateTime.now());

        mockMvc.perform(post("/messages/{id}/reply", messageId).with(as(receiver)).with(csrf()).param("content", "답입니다"))
                .andExpect(redirectedUrl("/messages?box=received&messageId=" + messageId))
                .andExpect(flash().attribute("toast", "답장을 보냈습니다"));
        long replyId = lastMessageId();
        assertThat(jdbcTemplate.queryForMap("SELECT sender_member_id, receiver_member_id, parent_message_id FROM message WHERE message_id = ?", replyId))
                .containsEntry("sender_member_id", receiverId).containsEntry("receiver_member_id", senderId)
                .containsEntry("parent_message_id", messageId);

        mockMvc.perform(post("/messages/{id}/reply", messageId).with(as(sender)).with(csrf()).param("content", "내가 보낸 것에 답"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 보내기_검사에_걸리면_같은_폼에_메시지와_입력값을_보여준다() throws Exception {
        write(sender, senderId, "나에게").andExpect(view().name("message/messages"))
                .andExpect(model().attributeHasErrors("messageWriteRequest"))
                .andExpect(content().string(allOf(containsString("나에게는 쪽지를 보낼 수 없습니다."), containsString("나에게"))));
        long withdrawnId = idOf(member(prefix + "탈퇴", true, "WITHDRAWN"));
        write(sender, withdrawnId, "탈퇴한 사람에게").andExpect(content().string(containsString("받는 사람을 찾을 수 없습니다.")));
        jdbcTemplate.update("INSERT INTO banned_word (word) VALUES (?)", "쪽지금칙");
        write(sender, receiverId, "쪽지금칙 들어감").andExpect(content().string(containsString("사용할 수 없는 단어가 들어 있습니다.")));
        String unverified = member(prefix + "미인증", false, "ACTIVE");
        write(unverified, receiverId, "인증 전").andExpect(content().string(containsString("이메일 인증을 마치면")));
        write(sender, receiverId, " ").andExpect(model().attributeHasFieldErrors("messageWriteRequest", "content"));
        mockMvc.perform(post("/messages").with(as(sender)).with(csrf()).param("content", "받는 사람 없음"))
                .andExpect(model().attributeHasFieldErrors("messageWriteRequest", "receiverMemberId"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message WHERE sender_member_id IN (?, ?)", Long.class,
                senderId, idOf(unverified))).isZero();
    }

    @Test
    void 오늘_20통을_보냈으면_막는다() throws Exception {
        for (int i = 0; i < 20; i++) {
            long other = idOf(member(prefix + "다" + i, true, "ACTIVE"));
            messageOf(senderId, other, null, "보낸 쪽지 " + i, LocalDateTime.now());
        }

        write(sender, receiverId, "21번째").andExpect(content().string(containsString("쪽지는 하루 20통까지 보낼 수 있습니다.")));
    }

    @Test
    void 답장_없는_상대에게_연속_3통이면_막고_답이_오면_다시_보낼_수_있다() throws Exception {
        for (int i = 0; i < 3; i++) {
            messageOf(senderId, receiverId, null, "연속 " + i, LocalDateTime.now());
        }
        write(sender, receiverId, "네 번째").andExpect(content().string(containsString("연속 3통까지 보낼 수 있습니다.")));

        messageOf(receiverId, senderId, null, "답이에요", LocalDateTime.now());
        write(sender, receiverId, "답 받고 다시").andExpect(flash().attribute("toast", "쪽지를 보냈습니다"));
    }

    @Test
    void 받는_사람은_닉네임_앞부분으로_찾고_나와_탈퇴_회원은_빼며_퍼센트는_글자_그대로_찾는다() throws Exception {
        member(prefix + "탈퇴", true, "WITHDRAWN");

        mockMvc.perform(get("/api/users").param("nickname", prefix).with(as(sender)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberId").value(receiverId))
                .andExpect(jsonPath("$[0].nickname").value(prefix + "받음"));
        mockMvc.perform(get("/api/users").param("nickname", "%").with(as(sender))).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/users").param("nickname", " ").with(as(sender))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void 공개_프로필에서_보내면_프로필로_돌아가고_실패하면_내용과_메시지를_돌려준다() throws Exception {
        mockMvc.perform(post("/users/{id}/messages", receiverId).with(as(sender)).with(csrf()).param("content", "프로필에서"))
                .andExpect(redirectedUrl("/users/" + receiverId)).andExpect(flash().attribute("toast", "쪽지를 보냈습니다"));
        mockMvc.perform(post("/users/{id}/messages", senderId).with(as(sender)).with(csrf()).param("content", "나에게"))
                .andExpect(redirectedUrl("/users/" + senderId))
                .andExpect(flash().attribute("messageContent", "나에게"))
                .andExpect(flash().attribute("messageError", "나에게는 쪽지를 보낼 수 없습니다."));
    }

    @Test
    void 일년_지난_쪽지는_지우고_그것을_가리키던_답장은_남긴다() {
        long old = messageOf(senderId, receiverId, null, "오래된 쪽지", LocalDateTime.now().minusYears(1).minusDays(1));
        long reply = messageOf(receiverId, senderId, old, "아직 남을 답장", LocalDateTime.now().minusMonths(11));

        messageService.deleteOld();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message WHERE message_id = ?", Long.class, old)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT parent_message_id FROM message WHERE message_id = ?", Long.class, reply)).isNull();
    }

    private ResultActions write(String email, long receiverMemberId, String text) throws Exception {
        return mockMvc.perform(post("/messages").with(as(email)).with(csrf())
                .param("receiverMemberId", String.valueOf(receiverMemberId)).param("content", text));
    }

    private long messageOf(long from, long to, Long parentId, String text, LocalDateTime createdAt) {
        jdbcTemplate.update("INSERT INTO message (sender_member_id, receiver_member_id, parent_message_id, content, created_at) "
                + "VALUES (?, ?, ?, ?, ?)", from, to, parentId, text, createdAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long lastMessageId() {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT MAX(message_id) FROM message", Long.class);
    }

    private LocalDateTime readAt(long messageId) {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT read_at FROM message WHERE message_id = ?", LocalDateTime.class, messageId);
    }

    private String member(String nickname, boolean verified, String status) {
        String email = "msg-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, status, email_verified_at) VALUES (?, 'x', ?, ?, ?)",
                email, nickname, status, verified ? LocalDateTime.now() : null);
        return email;
    }

    private long idOf(String email) {
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, email);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
