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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 공지 목록·상세, 운영자 공지 작성·수정, 헤더 배너 (API-NTF-04~08, FR-NTF-003~005, PL-NTF-003, #186).
 * 회원·공지는 테스트마다 새로 넣고 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoticeTest {

    private static final DateTimeFormatter FORM = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired EntityManager entityManager;

    private String admin;
    private long adminId;
    private String learner;

    @BeforeEach
    void 운영자와_학습자를_넣는다() {
        admin = member("ADMIN", "ACTIVE");
        adminId = idOf(admin);
        learner = member("LEARNER", "ACTIVE");
    }

    @Test
    void 공지를_쓰면_탈퇴하지_않은_회원_모두에게_알림이_가고_목록과_상세에_보인다() throws Exception {
        long withdrawnId = idOf(member("LEARNER", "WITHDRAWN"));

        write("11월 점검 안내", "본문입니다", false, null, null)
                .andExpect(redirectedUrl("/admin/notices")).andExpect(flash().attribute("toast", "공지를 등록했습니다"));
        entityManager.flush();
        long noticeId = jdbcTemplate.queryForObject("SELECT MAX(notice_id) FROM notice WHERE admin_member_id = ?", Long.class, adminId);

        assertThat(noticeAlerts(idOf(learner), noticeId)).isEqualTo(1);
        assertThat(noticeAlerts(adminId, noticeId)).isEqualTo(1);
        assertThat(noticeAlerts(withdrawnId, noticeId)).isZero();
        mockMvc.perform(get("/notices").with(as(learner)))
                .andExpect(content().string(allOf(containsString("11월 점검 안내"), containsString("/notices/" + noticeId))));
        mockMvc.perform(get("/notices/{id}", noticeId).with(as(learner)))
                .andExpect(content().string(allOf(containsString("11월 점검 안내"), containsString("본문입니다"))));
    }

    @Test
    void 비회원은_로그인으로_가고_없는_공지는_404() throws Exception {
        mockMvc.perform(get("/notices")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/notices/{id}", 999_999_999L).with(as(learner))).andExpect(status().isNotFound());
    }

    @Test
    void 제목_본문이_없거나_배너_기간이_틀리면_같은_화면에_메시지를_보여준다() throws Exception {
        write(" ", " ", false, null, null)
                .andExpect(view().name("message/admin-notices"))
                .andExpect(model().attributeHasFieldErrors("noticeForm", "title", "body"));
        LocalDateTime now = LocalDateTime.now();
        write("배너", "본문", true, now.plusDays(1), now)
                .andExpect(model().attributeHasFieldErrors("noticeForm", "bannerPeriodValid"));
        write("배너", "본문", true, null, null)
                .andExpect(model().attributeHasFieldErrors("noticeForm", "bannerPeriodValid"));
    }

    @Test
    void 배너_기간이_겹치면_막고_수정할_때는_자기_자신을_빼고_본다() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusYears(5).withSecond(0).withNano(0);
        long first = bannerNotice("첫 배너", start, start.plusDays(3));

        write("겹치는 배너", "본문", true, start.plusDays(1), start.plusDays(5))
                .andExpect(view().name("message/admin-notices"))
                .andExpect(model().attributeHasFieldErrorCode("noticeForm", "bannerStartsAt", "BANNER_PERIOD_OVERLAP"));
        write("이어지는 배너", "본문", true, start.plusDays(3), start.plusDays(4))
                .andExpect(redirectedUrl("/admin/notices"));
        edit(first, "첫 배너 고침", "본문", true, start, start.plusDays(3))
                .andExpect(redirectedUrl("/admin/notices")).andExpect(flash().attribute("toast", "공지를 수정했습니다"));

        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM notice WHERE notice_id = ?", String.class, first))
                .isEqualTo("첫 배너 고침");
    }

    @Test
    void 수정은_알림을_다시_보내지_않는다() throws Exception {
        long noticeId = bannerNotice("알림 없는 공지", null, null);

        edit(noticeId, "고친 제목", "고친 본문", false, null, null).andExpect(redirectedUrl("/admin/notices"));

        entityManager.flush();
        assertThat(noticeAlerts(idOf(learner), noticeId)).isZero();
    }

    @Test
    void 기간_안의_배너만_헤더에_보인다() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        long showing = bannerNotice("지금 배너", now.minusMinutes(1), now.plusHours(1));
        bannerNotice("지난 배너", now.minusDays(3), now.minusDays(2));

        mockMvc.perform(get("/community"))
                .andExpect(content().string(allOf(containsString("/notices/" + showing), containsString("지금 배너"),
                        not(containsString("지난 배너")))));
    }

    private ResultActions write(String title, String body, boolean banner, LocalDateTime startsAt, LocalDateTime endsAt) throws Exception {
        return mockMvc.perform(form(post("/admin/notices"), title, body, banner, startsAt, endsAt));
    }

    private ResultActions edit(long noticeId, String title, String body, boolean banner, LocalDateTime startsAt,
                               LocalDateTime endsAt) throws Exception {
        return mockMvc.perform(form(post("/admin/notices/{id}/edit", noticeId), title, body, banner, startsAt, endsAt));
    }

    private MockHttpServletRequestBuilder form(MockHttpServletRequestBuilder request, String title, String body, boolean banner,
                                               LocalDateTime startsAt, LocalDateTime endsAt) {
        request.with(as(admin)).with(csrf()).param("title", title).param("body", body);
        if (banner) {
            request.param("isBannerShown", "true");
        }
        if (startsAt != null) {
            request.param("bannerStartsAt", startsAt.format(FORM));
        }
        if (endsAt != null) {
            request.param("bannerEndsAt", endsAt.format(FORM));
        }
        return request;
    }

    /* startsAt 이 null 이면 배너가 아닌 공지 */
    private long bannerNotice(String title, LocalDateTime startsAt, LocalDateTime endsAt) {
        jdbcTemplate.update("INSERT INTO notice (admin_member_id, title, body, is_banner_shown, banner_starts_at, banner_ends_at) "
                + "VALUES (?, ?, '본문', ?, ?, ?)", adminId, title, startsAt != null, startsAt, endsAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long noticeAlerts(long memberId, long noticeId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE member_id = ? AND notification_type = 'NOTICE' "
                + "AND target_id = ?", Long.class, memberId, noticeId);
    }

    private String member(String role, String status) {
        String email = "notice-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, role, status, email_verified_at) "
                + "VALUES (?, 'x', '공지회원', ?, ?, NOW())", email, role, status);
        return email;
    }

    private long idOf(String email) {
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, email);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
