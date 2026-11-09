package com.vibelog.domain.contest.model.service;

import com.vibelog.domain.contest.model.dto.BadgeView;
import com.vibelog.domain.contest.model.policy.ContestEntryCanceler;
import com.vibelog.domain.contest.model.policy.ContestEntryChecker;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/*
 * contest 기반 (#176): 계약 ⑮ 출품 중인가, ⑯ 출품 취소, ContestReader 배지·이번 달 콘테스트.
 * "출품 중" = 취소 안 됨 + 투표가 끝나기 전 (contest 명세 결정 1). 개발 DB 와 월이 겹치지 않게 2091년을 쓴다.
 * 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@Transactional
class ContestContractServiceTest {

    @Autowired ContestEntryChecker contestEntryChecker;
    @Autowired ContestEntryCanceler contestEntryCanceler;
    @Autowired ContestReader contestReader;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    private long memberId;

    @BeforeEach
    void 회원을_넣는다() {
        memberId = member();
    }

    @Test
    void 투표가_끝나기_전_취소되지_않은_출품만_출품_중이다() {
        LocalDateTime now = LocalDateTime.now();
        long voting = contest("2091-01", now.minusDays(10), now.minusDays(1), now.minusDays(1), now.plusDays(3));
        long ended = contest("2091-02", now.minusDays(40), now.minusDays(30), now.minusDays(30), now.minusDays(1));
        long ongoing = portfolio(memberId);
        long finished = portfolio(memberId);
        long otherId = member();
        long canceled = portfolio(otherId);
        entry(voting, memberId, ongoing, null);
        entry(ended, memberId, finished, null);
        entry(voting, otherId, canceled, now.minusHours(1));

        assertThat(contestEntryChecker.isEntered(ongoing)).isTrue();
        assertThat(contestEntryChecker.isEntered(finished)).isFalse();
        assertThat(contestEntryChecker.isEntered(canceled)).isFalse();
        assertThat(contestEntryChecker.isEntered(portfolio(memberId))).isFalse();
    }

    @Test
    void 출품_취소는_출품_중인_것만_한다() {
        LocalDateTime now = LocalDateTime.now();
        long voting = contest("2091-03", now.minusDays(10), now.minusDays(1), now.minusDays(1), now.plusDays(3));
        long ended = contest("2091-04", now.minusDays(40), now.minusDays(30), now.minusDays(30), now.minusDays(1));
        long ongoing = portfolio(memberId);
        long finished = portfolio(memberId);
        long ongoingEntry = entry(voting, memberId, ongoing, null);
        long finishedEntry = entry(ended, memberId, finished, null);

        contestEntryCanceler.cancelEntryOf(ongoing);
        contestEntryCanceler.cancelEntryOf(finished);
        entityManager.flush();

        assertThat(canceledAt(ongoingEntry)).isNotNull();
        assertThat(canceledAt(finishedEntry)).isNull();
        assertThat(contestEntryChecker.isEntered(ongoing)).isFalse();
    }

    @Test
    void 배지는_월과_순위가_적힌_이름으로_최근_월부터_나온다() {
        LocalDateTime past = LocalDateTime.now().minusYears(1);
        long older = contest("2091-05", past, past.plusDays(1), past.plusDays(1), past.plusDays(2));
        long newer = contest("2091-11", past, past.plusDays(1), past.plusDays(1), past.plusDays(2));
        award(older, entry(older, memberId, portfolio(memberId), null), 2);
        award(newer, entry(newer, memberId, portfolio(memberId), null), 1);

        assertThat(contestReader.findBadgesOf(memberId)).extracting(BadgeView::name, BadgeView::rank)
                .containsExactly(tuple("2091년 11월 1등", 1), tuple("2091년 5월 2등", 2));
    }

    @Test
    void 이번_달_콘테스트를_돌려준다() {
        String month = YearMonth.now().toString();
        Long existing = jdbcTemplate.query("SELECT contest_id FROM contest WHERE contest_month = ?",
                rs -> rs.next() ? rs.getLong(1) : null, month);
        LocalDateTime start = YearMonth.now().atDay(1).atStartOfDay();
        long contestId = existing != null ? existing
                : contest(month, start, start.plusDays(20), start.plusDays(20), YearMonth.now().plusMonths(1).atDay(1).atStartOfDay());

        assertThat(contestReader.findCurrent()).hasValueSatisfying(c -> assertThat(c.contestId()).isEqualTo(contestId));
    }

    private long contest(String month, LocalDateTime entryStarts, LocalDateTime entryEnds, LocalDateTime voteStarts,
                         LocalDateTime voteEnds) {
        jdbcTemplate.update("INSERT INTO contest (contest_month, topic, description, entry_starts_at, entry_ends_at, vote_starts_at, vote_ends_at)"
                + " VALUES (?, '주제', '설명', ?, ?, ?, ?)", month, entryStarts, entryEnds, voteStarts, voteEnds);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long member() {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '출품회원', NOW())",
                "contest-" + UUID.randomUUID() + "@vibelog.test");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long portfolio(long owner) {
        jdbcTemplate.update("INSERT INTO portfolio (member_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public)"
                + " VALUES (?, 'PERSONAL', '작품', 'https://example.com', '요약', '회고', 'Claude', TRUE)", owner);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long entry(long contestId, long entrant, long portfolioId, LocalDateTime canceledAt) {
        jdbcTemplate.update("INSERT INTO contest_entry (contest_id, member_id, portfolio_id, canceled_at) VALUES (?, ?, ?, ?)",
                contestId, entrant, portfolioId, canceledAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void award(long contestId, long entryId, int rank) {
        jdbcTemplate.update("INSERT INTO contest_award (contest_id, contest_entry_id, award_rank) VALUES (?, ?, ?)", contestId, entryId, rank);
    }

    private LocalDateTime canceledAt(long entryId) {
        return jdbcTemplate.queryForObject("SELECT canceled_at FROM contest_entry WHERE contest_entry_id = ?", LocalDateTime.class, entryId);
    }
}
