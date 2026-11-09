package com.vibelog.domain.contest.model.service;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 결과 발표·배지·지난 콘테스트 (FR-CTS-004·005, FR-RWD-001·002, PL-CTS-005·006, #202).
 * 개발 DB 와 월이 겹치지 않게 2094년 월을 쓴다. 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContestAwardTest {

    @Autowired ContestAwardService contestAwardService;
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired EntityManager entityManager;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void 순위는_같은_표_수면_같은_순위_순위마다_먼저_출품한_2명_3등까지() {
        // 출품순 1~7. 표: 5, 5, 5, 3, 1, 1, 0 → 1등 1·2 (3은 세 번째라 못 받음), 2등 4, 3등 5·6, 7은 0표
        Map<Long, Long> votes = Map.of(1L, 5L, 2L, 5L, 3L, 5L, 4L, 3L, 5L, 1L, 6L, 1L, 7L, 0L);
        assertThat(ContestAwardService.rank(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L), votes))
                .containsExactly(Map.entry(1L, 1), Map.entry(2L, 1), Map.entry(4L, 2), Map.entry(5L, 3), Map.entry(6L, 3));
        // 4등 표 수는 받지 않는다
        assertThat(ContestAwardService.rank(List.of(1L, 2L, 3L, 4L), Map.of(1L, 4L, 2L, 3L, 3L, 2L, 4L, 1L))).doesNotContainKey(4L);
    }

    @Test
    void 투표가_끝나면_수상작을_남기고_수상자에게_배지_알림을_보낸다() throws Exception {
        long contestId = contest("2094-01", now.minusHours(1));
        long first = member();
        long second = member();
        long suspended = member();
        long canceledOwner = member();
        long lateSuspended = member();
        long withdrawn = member();
        long firstEntry = entry(contestId, first, portfolio(first, "1등 작품", true), null);
        long secondEntry = entry(contestId, second, portfolio(second, "2등 작품", true), null);
        long suspendedEntry = entry(contestId, suspended, portfolio(suspended, "정지 회원 작품", true), null);
        long canceledEntry = entry(contestId, canceledOwner, portfolio(canceledOwner, "취소된 작품", true), now.minusDays(2));
        long lateEntry = entry(contestId, lateSuspended, portfolio(lateSuspended, "투표 끝난 뒤 정지된 회원 작품", true), null);
        long withdrawnEntry = entry(contestId, withdrawn, portfolio(withdrawn, "탈퇴 회원 작품", true), null);
        votes(contestId, firstEntry, 3);
        votes(contestId, secondEntry, 2);
        votes(contestId, suspendedEntry, 9);
        votes(contestId, canceledEntry, 9);
        votes(contestId, lateEntry, 1);
        votes(contestId, withdrawnEntry, 9);
        // 정지·탈퇴는 투표가 끝난 시각(1시간 전)으로 본다: 그 전 정지·탈퇴는 빠지고, 그 뒤 정지는 그대로 받는다 (결정 8)
        suspend(suspended, first, now.minusDays(1));
        suspend(lateSuspended, first, now.minusMinutes(30));
        jdbcTemplate.update("UPDATE member SET status = 'WITHDRAWN', withdrawn_at = ? WHERE member_id = ?", now.minusHours(2), withdrawn);

        contestAwardService.announce();
        contestAwardService.announce(); // 이미 수상 기록이 있으면 다시 집계하지 않는다
        entityManager.flush();

        assertThat(jdbcTemplate.queryForList("SELECT contest_entry_id, award_rank FROM contest_award WHERE contest_id = ? ORDER BY award_rank",
                contestId)).extracting(row -> List.of(((Number) row.get("contest_entry_id")).longValue(), ((Number) row.get("award_rank")).intValue()))
                .containsExactly(List.of(firstEntry, 1), List.of(secondEntry, 2), List.of(lateEntry, 3));
        assertThat(jdbcTemplate.queryForObject("SELECT content FROM notification WHERE member_id = ? AND notification_type = 'BADGE'",
                String.class, first)).isEqualTo("배지를 받았습니다: 2094년 1월 1등");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE member_id = ? AND notification_type = 'BADGE'",
                Long.class, suspended)).isZero();

        // 배지는 마이페이지 배지 탭과 공개 프로필(U-29)에 보인다 (FR-RWD-002)
        mockMvc.perform(get("/mypage").param("tab", "badges").with(as(first)))
                .andExpect(content().string(containsString("2094년 1월 1등")));
        mockMvc.perform(get("/users/{id}", first).with(as(second)))
                .andExpect(content().string(containsString("2094년 1월 1등")));
    }

    @Test
    void 수상권에_자동_숨김_출품작이_있으면_기한_전에는_미루고_결과_화면에_보류를_알린다() throws Exception {
        long contestId = contest("2094-06", now.minusHours(1));
        long a = member();
        long b = member();
        long reported = portfolio(a, "신고된 작품", true);
        long aEntry = entry(contestId, a, reported, null);
        long bEntry = entry(contestId, b, portfolio(b, "보통 작품", true), null);
        votes(contestId, aEntry, 3);
        votes(contestId, bEntry, 1);
        report(reported, a, 3, "PENDING", null);
        long viewer = member();

        contestAwardService.announce();
        entityManager.flush();

        assertThat(awards(contestId)).isEmpty();
        mockMvc.perform(get("/contests/{id}", contestId).with(as(viewer)))
                .andExpect(content().string(allOf(containsString("신고 검토로 결과 발표가 미뤄졌습니다"), not(containsString("수상작이 없습니다")))));
        mockMvc.perform(get("/contests").with(as(viewer))).andExpect(content().string(containsString("발표 보류")));

        // 운영자가 기각하면 다음 배치에서 그대로 시상한다
        jdbcTemplate.update("UPDATE report SET status = 'REJECTED', handled_at = NOW() WHERE target_type = 'PORTFOLIO' AND target_id = ?", reported);
        entityManager.clear(); // 배치는 실행마다 새 트랜잭션이라 앞 실행에서 읽은 신고를 들고 있지 않는다
        contestAwardService.announce();
        entityManager.flush();

        assertThat(awards(contestId)).containsExactly(List.of(aEntry, 1), List.of(bEntry, 2));
    }

    @Test
    void 숨김_확정은_기한까지만_빼고_기한이_지나면_자동_숨김이어도_시상한다() {
        // 투표가 4일 전에 끝나 기한(투표 끝 + 3일)이 하루 전에 지났다
        LocalDateTime voteEnds = now.minusDays(4);
        long contestId = contest("2094-07", voteEnds);
        long a = member();
        long b = member();
        long c = member();
        long d = member();
        long hidden = portfolio(a, "기한 안에 숨긴 작품", true);
        long auto = portfolio(b, "자동 숨김 작품", true);
        long late = portfolio(d, "기한 뒤에 숨긴 작품", true);
        long aEntry = entry(contestId, a, hidden, null);
        long bEntry = entry(contestId, b, auto, null);
        long cEntry = entry(contestId, c, portfolio(c, "보통 작품", true), null);
        long dEntry = entry(contestId, d, late, null);
        votes(contestId, aEntry, 5);
        votes(contestId, dEntry, 4);
        votes(contestId, bEntry, 3);
        votes(contestId, cEntry, 1);
        report(hidden, a, 1, "HIDDEN", voteEnds.plusDays(1));
        report(auto, b, 3, "PENDING", null);
        report(late, d, 1, "HIDDEN", voteEnds.plusDays(3).plusHours(1));

        contestAwardService.announce();
        entityManager.flush();

        assertThat(awards(contestId)).containsExactly(List.of(dEntry, 1), List.of(bEntry, 2), List.of(cEntry, 3));
    }

    @Test
    void 투표가_끝난_지_오래돼도_수상_기록이_없으면_집계하고_투표_중이면_하지_않는다() {
        long old = contest("2094-02", now.minusDays(2));
        long voting = contestVoting("2094-03");
        long a = member();
        votes(old, entry(old, a, portfolio(a, "작품", true), null), 1);
        votes(voting, entry(voting, a, portfolio(a, "작품", true), null), 1);

        contestAwardService.announce();
        entityManager.flush();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest_award WHERE contest_id = ?", Long.class, old)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest_award WHERE contest_id = ?", Long.class, voting)).isZero();
    }

    @Test
    void 지난_콘테스트는_투표가_끝난_것만_보이고_비공개된_수상작은_제목을_숨긴다() throws Exception {
        long contestId = contest("2094-04", now.minusDays(3));
        long voting = contestVoting("2094-05");
        long owner = member();
        long hiddenOwner = member();
        long shown = portfolio(owner, "보이는 수상작", true);
        long hidden = portfolio(hiddenOwner, "숨긴 수상작", true);
        award(contestId, entry(contestId, owner, shown, null), 1);
        award(contestId, entry(contestId, hiddenOwner, hidden, null), 2);
        jdbcTemplate.update("UPDATE portfolio SET is_public = FALSE WHERE portfolio_id = ?", hidden);
        long viewer = member();

        mockMvc.perform(get("/contests").with(as(viewer)))
                .andExpect(content().string(allOf(containsString("2094-04"), containsString("1등 보이는 수상작"), not(containsString("2094-05")))));
        mockMvc.perform(get("/contests/{id}", contestId).with(as(viewer)))
                .andExpect(content().string(allOf(containsString("보이는 수상작"), containsString("/portfolios/" + shown),
                        containsString("비공개된 작품"), not(containsString("숨긴 수상작")), not(containsString("/portfolios/" + hidden)))));
        mockMvc.perform(get("/contests/{id}", voting).with(as(viewer))).andExpect(status().isNotFound());
    }

    @Test
    void 이달의_콘테스트_화면에_지난_달_결과를_보여준다() throws Exception {
        String lastMonth = YearMonth.now().minusMonths(1).toString();
        Long existing = jdbcTemplate.query("SELECT contest_id FROM contest WHERE contest_month = ?", rs -> rs.next() ? rs.getLong(1) : null, lastMonth);
        long contestId = existing != null ? existing : contest(lastMonth, now.minusDays(1));
        jdbcTemplate.update("UPDATE contest SET vote_ends_at = ? WHERE contest_id = ?", now.minusMinutes(1), contestId);
        long owner = member();
        award(contestId, entry(contestId, owner, portfolio(owner, "지난 달 수상작", true), null), 1);

        mockMvc.perform(get("/contests/current").with(as(member())))
                .andExpect(content().string(allOf(containsString(lastMonth + " 결과"), containsString("지난 달 수상작"))));
    }

    /* 투표가 voteEnds 에 끝난 콘테스트 (기간 순서는 ck_contest_period) */
    private long contest(String month, LocalDateTime voteEnds) {
        LocalDateTime voteStarts = voteEnds.minusDays(10);
        return insertContest(month, voteStarts.minusDays(20), voteStarts, voteStarts, voteEnds);
    }

    private long contestVoting(String month) {
        return insertContest(month, now.minusDays(2), now.minusDays(1), now.minusDays(1), now.plusDays(1));
    }

    private long insertContest(String month, LocalDateTime entryStarts, LocalDateTime entryEnds, LocalDateTime voteStarts, LocalDateTime voteEnds) {
        jdbcTemplate.update("INSERT INTO contest (contest_month, topic, description, entry_starts_at, entry_ends_at, vote_starts_at, vote_ends_at)"
                + " VALUES (?, '주제', '설명', ?, ?, ?, ?)", month, entryStarts, entryEnds, voteStarts, voteEnds);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long member() {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '수상후보', NOW())",
                "contest-award-" + UUID.randomUUID() + "@vibelog.test");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long portfolio(long owner, String title, boolean isPublic) {
        jdbcTemplate.update("INSERT INTO portfolio (member_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public)"
                + " VALUES (?, 'PERSONAL', ?, 'https://example.com', '요약', '회고', 'Claude', ?)", owner, title, isPublic);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long entry(long contestId, long entrant, long portfolioId, LocalDateTime canceledAt) {
        jdbcTemplate.update("INSERT INTO contest_entry (contest_id, member_id, portfolio_id, canceled_at) VALUES (?, ?, ?, ?)",
                contestId, entrant, portfolioId, canceledAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /* 서로 다른 투표자 count 명이 한 표씩 */
    private void votes(long contestId, long entryId, int count) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update("INSERT INTO contest_vote (contest_id, contest_entry_id, voter_member_id, vote_date) VALUES (?, ?, ?, ?)",
                    contestId, entryId, member(), LocalDate.now().minusDays(1));
        }
    }

    private void suspend(long memberId, long adminId, LocalDateTime startsAt) {
        jdbcTemplate.update("INSERT INTO member_suspension (member_id, admin_member_id, level, reason, starts_at) VALUES (?, ?, 'PERMANENT', '사유', ?)",
                memberId, adminId, startsAt);
    }

    /* (출품작 ID, 순위) 순위순 */
    private List<List<Object>> awards(long contestId) {
        return jdbcTemplate.queryForList("SELECT contest_entry_id, award_rank FROM contest_award WHERE contest_id = ? ORDER BY award_rank, contest_entry_id",
                contestId).stream()
                .map(row -> List.<Object>of(((Number) row.get("contest_entry_id")).longValue(), ((Number) row.get("award_rank")).intValue()))
                .toList();
    }

    /* 서로 다른 신고자 count 명이 그 작품을 status 로 신고 (처리된 신고면 handledAt) */
    private void report(long portfolioId, long ownerId, int count, String status, LocalDateTime handledAt) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update("INSERT INTO report (reporter_member_id, target_type, target_id, target_member_id, reason, status, handled_at)"
                    + " VALUES (?, 'PORTFOLIO', ?, ?, 'SPAM', ?, ?)", member(), portfolioId, ownerId, status, handledAt);
        }
    }

    private void award(long contestId, long entryId, int rank) {
        jdbcTemplate.update("INSERT INTO contest_award (contest_id, contest_entry_id, award_rank) VALUES (?, ?, ?)", contestId, entryId, rank);
    }

    private RequestPostProcessor as(long memberId) {
        String email = jdbcTemplate.queryForObject("SELECT email FROM member WHERE member_id = ?", String.class, memberId);
        return user(memberLoginService.loadUserByUsername(email));
    }
}
