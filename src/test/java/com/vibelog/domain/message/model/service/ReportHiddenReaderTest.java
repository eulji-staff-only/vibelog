package com.vibelog.domain.message.model.service;

import com.vibelog.domain.message.model.dao.ReportRepository;
import com.vibelog.domain.message.model.entity.Report;
import com.vibelog.domain.message.model.entity.ReportReason;
import com.vibelog.domain.message.model.entity.ReportTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 신고 숨김 여부 계산 (MessageReader, FR-RPT-002, PL-RPT-002, PL-CTS-006, #254).
 * 대상 ID 는 다른 테이블과 이어지지 않아 큰 값을 쓴다. 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@Transactional
class ReportHiddenReaderTest {

    @Autowired MessageReader messageReader;
    @Autowired ReportRepository reportRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private final LocalDateTime now = LocalDateTime.now();
    private long author;

    @BeforeEach
    void setUp() {
        author = member();
    }

    @Test
    void 처리_대기_신고가_3건이면_자동_숨김이고_2건이면_아니다() {
        report("POST", 900_001L, 3, "PENDING", null);
        report("POST", 900_002L, 2, "PENDING", null);

        assertThat(messageReader.findHiddenTargetIds("POST", List.of(900_001L, 900_002L, 900_003L))).containsExactly(900_001L);
        assertThat(messageReader.findAutoHiddenTargetIds("POST", List.of(900_001L, 900_002L))).containsExactly(900_001L);
        assertThat(messageReader.findConfirmedHiddenTargetIds("POST", List.of(900_001L), now)).isEmpty();
    }

    @Test
    void 운영자가_숨긴_대상은_1건이어도_숨김이고_자동_숨김에서는_빠진다() {
        report("PORTFOLIO", 900_011L, 1, "HIDDEN", now.minusHours(1));

        assertThat(messageReader.findHiddenTargetIds("PORTFOLIO", List.of(900_011L))).containsExactly(900_011L);
        assertThat(messageReader.findConfirmedHiddenTargetIds("PORTFOLIO", List.of(900_011L), now)).containsExactly(900_011L);
        assertThat(messageReader.findAutoHiddenTargetIds("PORTFOLIO", List.of(900_011L))).isEmpty();
    }

    @Test
    void 기각한_신고는_세지_않고_그_뒤_신고는_0건부터_센다() {
        report("COMMENT", 900_021L, 3, "REJECTED", now.minusDays(1));
        report("COMMENT", 900_021L, 2, "PENDING", null);

        assertThat(messageReader.findHiddenTargetIds("COMMENT", List.of(900_021L))).isEmpty();
    }

    @Test
    void 숨김_확정은_기준_시각까지_처리된_것만_본다() {
        report("PORTFOLIO", 900_031L, 1, "HIDDEN", now.minusHours(1));

        assertThat(messageReader.findConfirmedHiddenTargetIds("PORTFOLIO", List.of(900_031L), now.minusHours(2))).isEmpty();
    }

    @Test
    void 대상_종류가_다르면_따로_센다() {
        report("POST", 900_041L, 3, "PENDING", null);

        assertThat(messageReader.findHiddenTargetIds("REVIEW", List.of(900_041L))).isEmpty();
        assertThat(messageReader.findHiddenTargetIds("POST", List.of())).isEmpty();
    }

    @Test
    void 신고를_만들면_처리_대기로_저장된다() {
        long reporter = member();
        Report saved = reportRepository.saveAndFlush(Report.create(reporter, ReportTargetType.MESSAGE, 900_051L, author, ReportReason.ETC, "기타 내용"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM report WHERE report_id = ?", String.class, saved.getReportId()))
                .isEqualTo("PENDING");
    }

    /* 서로 다른 신고자 count 명이 같은 상태로 신고한 행을 넣는다 (1인 1회, uk_report_reporter_target) */
    private void report(String targetType, long targetId, int count, String status, LocalDateTime handledAt) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update("INSERT INTO report (reporter_member_id, target_type, target_id, target_member_id, reason, status, handled_at)"
                    + " VALUES (?, ?, ?, ?, 'SPAM', ?, ?)", member(), targetType, targetId, author, status, handledAt);
        }
    }

    private long member() {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '신고회원', NOW())",
                "report-" + UUID.randomUUID() + "@vibelog.test");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
