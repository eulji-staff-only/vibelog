package com.vibelog.domain.contest.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
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

import java.time.LocalDateTime;
import java.time.YearMonth;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 이달의 콘테스트·출품·투표 (U-32, API-CTS-01~03, PL-CTS-002~004, #201).
 * 출품·투표 API 는 ID 로 찾으므로 개발 DB 와 겹치지 않게 2093년 월을 쓴다. 이달의 콘테스트 화면은 이번 달 콘테스트가 필요해
 * 이미 있으면 그 기간을 바꿔 쓴다. 넣고 바꾼 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContestEnterVoteTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void 출품_단계에는_내_공개_포트폴리오를_고르고_출품하면_내_출품이_보인다() throws Exception {
        long contestId = currentContest(now.minusDays(1), now.plusDays(1), now.plusDays(1), now.plusDays(2));
        String me = member(true);
        long open = portfolio(me, "출품할 공개 작품", true);
        portfolio(me, "숨긴 비공개 작품", false);

        mockMvc.perform(get("/contests/current").with(as(me)))
                .andExpect(content().string(allOf(containsString("출품할 공개 작품"), not(containsString("숨긴 비공개 작품")))));

        enter(contestId, me, open).andExpect(redirectedUrl("/contests/current")).andExpect(flash().attribute("toast", "출품했습니다"));
        mockMvc.perform(get("/contests/current").with(as(me)))
                .andExpect(content().string(allOf(containsString("출품한 작품"), containsString("출품할 공개 작품"))));
    }

    @Test
    void 출품은_출품_기간에_내_공개_작품_하나만_된다() throws Exception {
        long entryOpen = contest("2093-01", now.minusDays(1), now.plusDays(1), now.plusDays(1), now.plusDays(2));
        long voting = contest("2093-02", now.minusDays(2), now.minusDays(1), now.minusDays(1), now.plusDays(1));
        String me = member(true);
        String other = member(true);

        enter(entryOpen, me, portfolio(other, "남의 작품", true)).andExpect(flash().attribute("toastError", "본인만 할 수 있습니다."));
        enter(entryOpen, me, portfolio(me, "비공개", false)).andExpect(flash().attribute("toastError", "비공개된 작품입니다."));
        long deleted = portfolio(me, "삭제한 작품", true);
        jdbcTemplate.update("UPDATE portfolio SET deleted_at = NOW() WHERE portfolio_id = ?", deleted);
        enter(entryOpen, me, deleted).andExpect(flash().attribute("toastError", "찾을 수 없습니다."));
        enter(voting, me, portfolio(me, "기간 밖", true)).andExpect(flash().attribute("toastError", "지금은 출품 기간이 아닙니다."));
        enter(-1L, me, portfolio(me, "없는 콘테스트", true)).andExpect(flash().attribute("toastError", "콘테스트를 찾을 수 없습니다."));

        enter(entryOpen, me, portfolio(me, "첫 작품", true)).andExpect(flash().attribute("toast", "출품했습니다"));
        enter(entryOpen, me, portfolio(me, "두 번째 작품", true)).andExpect(flash().attribute("toastError", "이번 달 콘테스트에는 이미 출품했습니다."));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest_entry WHERE contest_id = ?", Long.class, entryOpen)).isEqualTo(1);
    }

    @Test
    void 투표는_하루_1표이고_같은_작품에는_1번만_내_작품에는_못_한다() throws Exception {
        long contestId = contest("2093-03", now.minusDays(2), now.minusDays(1), now.minusDays(1), now.plusDays(1));
        String a = member(true);
        String b = member(true);
        long first = entry(contestId, a, portfolio(a, "A 작품", true), null);
        long second = entry(contestId, b, portfolio(b, "B 작품", true), null);
        String voter = member(true);

        vote(contestId, voter, first).andExpect(status().isOk())
                .andExpect(jsonPath("$.contestEntryId").value(first)).andExpect(jsonPath("$.votedToday").value(true));
        vote(contestId, voter, second).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("DAILY_VOTE_LIMIT"));

        // 어제 한 표로 바꾸면 오늘 다시 1표. 같은 작품에는 기간 동안 1번만 (PL-CTS-003)
        jdbcTemplate.update("UPDATE contest_vote SET vote_date = vote_date - INTERVAL 1 DAY WHERE contest_entry_id = ?", first);
        vote(contestId, voter, first).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_VOTED"));
        vote(contestId, voter, second).andExpect(status().isOk());

        vote(contestId, a, first).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SELF_VOTE"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest_vote WHERE contest_id = ?", Long.class, contestId)).isEqualTo(2);
    }

    @Test
    void 투표는_투표_기간에_인증한_회원이_취소되지_않은_이_콘테스트_출품작에만_한다() throws Exception {
        long voting = contest("2093-04", now.minusDays(2), now.minusDays(1), now.minusDays(1), now.plusDays(1));
        long entryOpen = contest("2093-05", now.minusDays(1), now.plusDays(1), now.plusDays(1), now.plusDays(2));
        String a = member(true);
        long live = entry(voting, a, portfolio(a, "작품", true), null);
        String b = member(true);
        long canceled = entry(voting, b, portfolio(b, "취소된 작품", true), now.minusHours(1));
        long otherContest = entry(entryOpen, a, portfolio(a, "다른 콘테스트 작품", true), null);
        String voter = member(true);

        vote(voting, member(false), live).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        vote(entryOpen, voter, otherContest).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VOTE_CLOSED"));
        vote(voting, voter, canceled).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND"));
        vote(voting, voter, otherContest).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND"));
        vote(-1L, voter, live).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CONTEST_NOT_FOUND"));
    }

    @Test
    void 투표_단계에는_출품작을_보여주고_내_작품과_내가_투표한_작품을_표시한다() throws Exception {
        long contestId = currentContest(now.minusDays(2), now.minusDays(1), now.minusDays(1), now.plusDays(1));
        String me = member(true);
        String other = member(true);
        entry(contestId, me, portfolio(me, "내가 낸 작품", true), null);
        long votedEntry = entry(contestId, other, portfolio(other, "투표한 작품", true), null);
        String third = member(true);
        String fourth = member(true);
        entry(contestId, fourth, portfolio(fourth, "출품 취소된 작품", true), now.minusHours(1));
        entry(contestId, third, portfolio(third, "비공개로 바꾼 작품", false), null);
        vote(contestId, me, votedEntry).andExpect(status().isOk());

        mockMvc.perform(get("/contests/current").with(as(me)))
                .andExpect(content().string(allOf(containsString("내가 낸 작품"), containsString("내 작품"),
                        containsString("투표한 작품"), containsString("투표함"), containsString("오늘 투표를 마쳤습니다"),
                        not(containsString("출품 취소된 작품")), not(containsString("비공개로 바꾼 작품")))));
    }

    private ResultActions enter(long contestId, String email, long portfolioId) throws Exception {
        return mockMvc.perform(post("/contests/{id}/entries", contestId).param("portfolioId", String.valueOf(portfolioId))
                .with(as(email)).with(csrf()));
    }

    private ResultActions vote(long contestId, String email, long entryId) throws Exception {
        return mockMvc.perform(post("/api/contests/{id}/votes", contestId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contestEntryId\":" + entryId + "}").with(as(email)).with(csrf()));
    }

    /* 이번 달 콘테스트. 개발 DB 에 이미 있으면 기간만 바꾼다 (롤백됨) */
    private long currentContest(LocalDateTime entryStarts, LocalDateTime entryEnds, LocalDateTime voteStarts, LocalDateTime voteEnds) {
        String month = YearMonth.now().toString();
        Long existing = jdbcTemplate.query("SELECT contest_id FROM contest WHERE contest_month = ?", rs -> rs.next() ? rs.getLong(1) : null, month);
        if (existing == null) {
            return contest(month, entryStarts, entryEnds, voteStarts, voteEnds);
        }
        jdbcTemplate.update("UPDATE contest SET entry_starts_at = ?, entry_ends_at = ?, vote_starts_at = ?, vote_ends_at = ? WHERE contest_id = ?",
                entryStarts, entryEnds, voteStarts, voteEnds, existing);
        return existing;
    }

    private long contest(String month, LocalDateTime entryStarts, LocalDateTime entryEnds, LocalDateTime voteStarts, LocalDateTime voteEnds) {
        jdbcTemplate.update("INSERT INTO contest (contest_month, topic, description, entry_starts_at, entry_ends_at, vote_starts_at, vote_ends_at)"
                + " VALUES (?, '주제', '설명', ?, ?, ?, ?)", month, entryStarts, entryEnds, voteStarts, voteEnds);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String member(boolean verified) {
        String email = "contest-vote-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '참가자', ?)",
                email, verified ? now : null);
        return email;
    }

    private long portfolio(String ownerEmail, String title, boolean isPublic) {
        jdbcTemplate.update("INSERT INTO portfolio (member_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public)"
                + " SELECT member_id, 'PERSONAL', ?, 'https://example.com', '요약', '회고', 'Claude', ? FROM member WHERE email = ?",
                title, isPublic, ownerEmail);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long entry(long contestId, String entrantEmail, long portfolioId, LocalDateTime canceledAt) {
        jdbcTemplate.update("INSERT INTO contest_entry (contest_id, member_id, portfolio_id, canceled_at) SELECT ?, member_id, ?, ? FROM member"
                + " WHERE email = ?", contestId, portfolioId, canceledAt, entrantEmail);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
