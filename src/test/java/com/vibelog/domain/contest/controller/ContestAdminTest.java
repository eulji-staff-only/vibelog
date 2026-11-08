package com.vibelog.domain.contest.controller;

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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 백오피스 콘테스트 관리 (A-11, API-CTS-06~08, FR-CTS-006, #200). 개발 DB 와 월이 겹치지 않게 2092년을 쓴다.
 * 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContestAdminTest {

    private static final DateTimeFormatter FORM = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired EntityManager entityManager;

    private String admin;

    @BeforeEach
    void 운영자를_넣는다() {
        admin = "contest-admin-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, role, email_verified_at) VALUES (?, 'x', '운영자', 'ADMIN', NOW())", admin);
    }

    @Test
    void 새_폼은_다음_달과_기본_기간으로_채운다() throws Exception {
        YearMonth next = YearMonth.now().plusMonths(1);
        mockMvc.perform(get("/admin/contests").with(as(admin)))
                .andExpect(content().string(allOf(containsString("value=\"" + next + "\""),
                        containsString("value=\"" + next.atDay(21).atStartOfDay().format(FORM) + "\""))));
    }

    @Test
    void 등록하면_목록에_보이고_같은_달은_다시_등록할_수_없다() throws Exception {
        save(post("/admin/contests"), "2092-01", "나만의 첫 서비스", YearMonth.of(2092, 1))
                .andExpect(redirectedUrl("/admin/contests")).andExpect(flash().attribute("toast", "콘테스트를 등록했습니다"));
        mockMvc.perform(get("/admin/contests").with(as(admin))).andExpect(content().string(containsString("나만의 첫 서비스")));

        save(post("/admin/contests"), "2092-01", "같은 달", YearMonth.of(2092, 1))
                .andExpect(view().name("contest/admin-contests"))
                .andExpect(content().string(containsString("그 달의 콘테스트가 이미 있습니다.")));
    }

    @Test
    void 빈_칸과_기간_순서가_틀리면_같은_화면에_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/contests").with(as(admin)).with(csrf()).param("contestMonth", "2092-02").param("topic", " ")
                        .param("description", "설명")
                        .param("entryStartsAt", "2092-02-21T00:00").param("entryEndsAt", "2092-02-01T00:00")
                        .param("voteStartsAt", "2092-02-21T00:00").param("voteEndsAt", "2092-03-01T00:00"))
                .andExpect(view().name("contest/admin-contests"))
                .andExpect(model().attributeHasFieldErrors("contestForm", "topic", "periodValid"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest WHERE contest_month = '2092-02'", Long.class)).isZero();
    }

    @Test
    void 출품이_시작되면_월과_기간은_못_바꾸고_주제는_바꾼다() throws Exception {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0); // 폼은 분 단위라 저장값도 분 단위로 맞춘다
        long started = contest("2092-03", now.minusDays(1), now.plusDays(10), now.plusDays(10), now.plusDays(20));
        long notYet = contest("2092-04", now.plusDays(30), now.plusDays(40), now.plusDays(40), now.plusDays(50));

        mockMvc.perform(form(post("/admin/contests/{id}/edit", started), "2092-03", "바뀐 주제",
                        now.minusDays(1), now.plusDays(11), now.plusDays(11), now.plusDays(20)))
                .andExpect(content().string(containsString("출품이 시작된 콘테스트는 월과 기간을 바꿀 수 없습니다.")));
        mockMvc.perform(form(post("/admin/contests/{id}/edit", started), "2092-03", "주제만 바꿈",
                        now.minusDays(1), now.plusDays(10), now.plusDays(10), now.plusDays(20)))
                .andExpect(flash().attribute("toast", "콘테스트를 수정했습니다"));
        save(post("/admin/contests/{id}/edit", notYet), "2092-05", "달을 옮김", YearMonth.of(2092, 5))
                .andExpect(flash().attribute("toast", "콘테스트를 수정했습니다"));

        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT topic FROM contest WHERE contest_id = ?", String.class, started)).isEqualTo("주제만 바꿈");
        assertThat(jdbcTemplate.queryForObject("SELECT contest_month FROM contest WHERE contest_id = ?", String.class, notYet)).isEqualTo("2092-05");
    }

    @Test
    void 운영자만_들어온다() throws Exception {
        String learner = "contest-learner-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '학습자', NOW())", learner);
        // 운영자가 아니면 메인으로 보낸다 (SecurityConfig ADMIN_ONLY)
        mockMvc.perform(get("/admin/contests").with(as(learner))).andExpect(redirectedUrl("/"));
    }

    /* PL-CTS-001 기본 기간으로 저장 */
    private ResultActions save(MockHttpServletRequestBuilder request, String month, String topic, YearMonth ym) throws Exception {
        return mockMvc.perform(form(request, month, topic, ym.atDay(1).atStartOfDay(), ym.atDay(21).atStartOfDay(),
                ym.atDay(21).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay()));
    }

    private MockHttpServletRequestBuilder form(MockHttpServletRequestBuilder request, String month, String topic, LocalDateTime entryStarts,
                                               LocalDateTime entryEnds, LocalDateTime voteStarts, LocalDateTime voteEnds) {
        return request.with(as(admin)).with(csrf()).param("contestMonth", month).param("topic", topic).param("description", "설명")
                .param("entryStartsAt", entryStarts.format(FORM)).param("entryEndsAt", entryEnds.format(FORM))
                .param("voteStartsAt", voteStarts.format(FORM)).param("voteEndsAt", voteEnds.format(FORM));
    }

    private long contest(String month, LocalDateTime entryStarts, LocalDateTime entryEnds, LocalDateTime voteStarts, LocalDateTime voteEnds) {
        jdbcTemplate.update("INSERT INTO contest (contest_month, topic, description, entry_starts_at, entry_ends_at, vote_starts_at, vote_ends_at)"
                + " VALUES (?, '주제', '설명', ?, ?, ?, ?)", month, entryStarts, entryEnds, voteStarts, voteEnds);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
