package com.vibelog.domain.admin.model.service;

import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.contest.model.service.ContestReader;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.message.model.service.MessageReader;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 확장 대시보드(API-ADM-40, FR-ADM-003, #194 1차)가 쓰는 각 영역 Reader 의 기간별 건수. 테스트가 끝나면 롤백된다.
 * 기간 P = 2020-05-01 00:00 ~ 2020-05-31 23:59:59 (시드 데이터는 모두 이후 날짜라 겹치지 않는다).
 *
 * 진단: 기간 안 완료 2·건너뜀 1, 기간 밖(6월) 완료 1
 * 포트폴리오: 기간 안 2, 삭제 표시 1(기간 안), 기간 밖 1
 * 게시글: 기간 안 2, 삭제 표시 1, 기간 밖 1
 * 동료 리뷰: 기간 안 2, 삭제 표시 1, 기간 밖 1
 * 쪽지: 기간 안 보낸·받은 사람 모두 안 지움 1 + 받은 사람만 지움 1 = 2, 둘 다 지움 1(제외), 기간 밖 1
 * 신고: 기간 안 처리 대기 1 + 기각 1 = 2, 기간 밖 1
 * 콘테스트 출품: 기간 안 2, 취소 1, 기간 밖 1
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES (8001, 'dash-r1@vibelog.test', '집계1'), (8002, 'dash-r2@vibelog.test', '집계2'), "
                + "(8003, 'dash-r3@vibelog.test', '집계3'), (8004, 'dash-r4@vibelog.test', '집계4')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (8001, '집계트랙', '대상', '설명', 8001)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (8001, '집계분류', 8001)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES "
                + "(8001, 8001, 8001, '집계코스', '요약', '설명', '대상', '목표', 7)",
        // 진단
        "INSERT INTO diagnosis_result (member_id, recommended_track_id, status, created_at) VALUES "
                + "(8001, 8001, 'COMPLETED', '2020-05-10 10:00:00'), (8002, 8001, 'COMPLETED', '2020-05-31 23:00:00'), "
                + "(8003, NULL, 'SKIPPED', '2020-05-01 00:00:00'), (8004, 8001, 'COMPLETED', '2020-06-01 00:00:00')",
        // 포트폴리오
        "INSERT INTO portfolio (portfolio_id, member_id, course_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public, created_at, deleted_at) VALUES "
                + "(8001, 8001, 8001, 'COURSE', '작품1', 'http://x', '요약', '회고', 'AI', TRUE, '2020-05-10 10:00:00', NULL), "
                + "(8002, 8002, 8001, 'COURSE', '작품2', 'http://x', '요약', '회고', 'AI', TRUE, '2020-05-31 23:00:00', NULL), "
                + "(8003, 8001, NULL, 'PERSONAL', '삭제작품', 'http://x', '요약', '회고', 'AI', TRUE, '2020-05-12 10:00:00', '2020-05-13 10:00:00'), "
                + "(8004, 8002, NULL, 'PERSONAL', '밖작품', 'http://x', '요약', '회고', 'AI', TRUE, '2020-06-02 10:00:00', NULL)",
        // 게시글
        "INSERT INTO post (post_id, member_id, category, title, body, created_at, deleted_at) VALUES "
                + "(8001, 8001, 'FREE', '글1', '본문', '2020-05-02 10:00:00', NULL), (8002, 8002, 'FREE', '글2', '본문', '2020-05-31 22:00:00', NULL), "
                + "(8003, 8001, 'FREE', '삭제글', '본문', '2020-05-03 10:00:00', '2020-05-04 10:00:00'), (8004, 8001, 'FREE', '밖글', '본문', '2020-06-03 10:00:00', NULL)",
        // 동료 리뷰
        "INSERT INTO review_request (review_request_id, portfolio_id, requested_at) VALUES (8001, 8001, '2020-05-05 10:00:00'), (8002, 8002, '2020-05-06 10:00:00')",
        "INSERT INTO peer_review (peer_review_id, review_request_id, reviewer_member_id, created_at, deleted_at) VALUES "
                + "(8001, 8001, 8002, '2020-05-07 10:00:00', NULL), (8002, 8001, 8003, '2020-05-08 10:00:00', NULL), "
                + "(8003, 8001, 8004, '2020-05-09 10:00:00', '2020-05-10 10:00:00'), (8004, 8002, 8001, '2020-06-04 10:00:00', NULL)",
        // 쪽지
        "INSERT INTO message (message_id, sender_member_id, receiver_member_id, content, sender_deleted_at, receiver_deleted_at, created_at) VALUES "
                + "(8001, 8001, 8002, '안녕', NULL, NULL, '2020-05-11 10:00:00'), "
                + "(8002, 8001, 8002, '안녕2', NULL, '2020-05-13 10:00:00', '2020-05-12 10:00:00'), "
                + "(8003, 8001, 8002, '안녕3', '2020-05-14 10:00:00', '2020-05-15 10:00:00', '2020-05-13 10:00:00'), "
                + "(8004, 8001, 8002, '안녕4', NULL, NULL, '2020-06-05 10:00:00')",
        // 신고
        "INSERT INTO report (report_id, reporter_member_id, target_type, target_id, target_member_id, reason, status, created_at) VALUES "
                + "(8001, 8001, 'POST', 8001, 8002, 'SPAM', 'PENDING', '2020-05-14 10:00:00'), "
                + "(8002, 8001, 'POST', 8002, 8002, 'SPAM', 'REJECTED', '2020-05-15 10:00:00'), "
                + "(8003, 8001, 'POST', 8003, 8002, 'SPAM', 'PENDING', '2020-06-06 10:00:00')",
        // 콘테스트
        "INSERT INTO contest (contest_id, contest_month, topic, description, entry_starts_at, entry_ends_at, vote_starts_at, vote_ends_at) VALUES "
                + "(8001, '2020-05', '주제', '설명', '2020-05-01 00:00:00', '2020-05-20 00:00:00', '2020-05-21 00:00:00', '2020-05-31 00:00:00')",
        "INSERT INTO contest_entry (contest_entry_id, contest_id, member_id, portfolio_id, canceled_at, created_at) VALUES "
                + "(8001, 8001, 8001, 8001, NULL, '2020-05-16 10:00:00'), (8002, 8001, 8002, 8002, NULL, '2020-05-31 23:00:00'), "
                + "(8003, 8001, 8003, 8001, '2020-05-18 10:00:00', '2020-05-17 10:00:00'), (8004, 8001, 8004, 8001, NULL, '2020-06-07 10:00:00')"
})
class DashboardReaderCountTest {

    static final LocalDateTime FROM = LocalDateTime.of(2020, 5, 1, 0, 0);
    static final LocalDateTime TO = LocalDateTime.of(2020, 5, 31, 23, 59, 59);

    @Autowired CourseReader courseReader;
    @Autowired PortfolioReader portfolioReader;
    @Autowired CommunityReader communityReader;
    @Autowired MessageReader messageReader;
    @Autowired ContestReader contestReader;

    @Test
    void 진단_결과는_기간_안의_완료와_건너뜀을_센다() {
        Map<String, Integer> counts = courseReader.countDiagnosisResultsByStatus(FROM, TO);

        assertThat(counts).containsEntry("COMPLETED", 2).containsEntry("SKIPPED", 1);
    }

    @Test
    void 진단_결과가_없는_기간이어도_두_키는_0으로_있다() {
        Map<String, Integer> counts = courseReader.countDiagnosisResultsByStatus(
                LocalDateTime.of(2019, 1, 1, 0, 0), LocalDateTime.of(2019, 1, 31, 23, 59));

        assertThat(counts).containsEntry("COMPLETED", 0).containsEntry("SKIPPED", 0);
    }

    @Test
    void 진단_결과는_시작일_종료일이_null이면_그_쪽_제한이_없다() {
        assertThat(courseReader.countDiagnosisResultsByStatus(null, TO)).containsEntry("COMPLETED", 2).containsEntry("SKIPPED", 1);
        assertThat(courseReader.countDiagnosisResultsByStatus(FROM, null).get("COMPLETED")).isGreaterThanOrEqualTo(3);
        assertThat(courseReader.countDiagnosisResultsByStatus(null, null).get("COMPLETED")).isGreaterThanOrEqualTo(3);
    }

    @Test
    void 포트폴리오_제출_수는_삭제_표시와_기간_밖을_뺀다() {
        assertThat(portfolioReader.countSubmitted(FROM, TO)).isEqualTo(2);
        assertThat(portfolioReader.countSubmitted(null, TO)).isEqualTo(2);
        assertThat(portfolioReader.countSubmitted(FROM, null)).isGreaterThanOrEqualTo(3);
        assertThat(portfolioReader.countSubmitted(null, null)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void 게시글_수는_삭제_표시와_기간_밖을_뺀다() {
        assertThat(communityReader.countPosts(FROM, TO)).isEqualTo(2);
        assertThat(communityReader.countPosts(null, TO)).isEqualTo(2);
        assertThat(communityReader.countPosts(FROM, null)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void 동료_리뷰_수는_삭제_표시와_기간_밖을_뺀다() {
        assertThat(communityReader.countPeerReviews(FROM, TO)).isEqualTo(2);
        assertThat(communityReader.countPeerReviews(null, TO)).isEqualTo(2);
        assertThat(communityReader.countPeerReviews(FROM, null)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void 쪽지_수는_양쪽이_모두_지운_쪽지와_기간_밖을_뺀다() {
        assertThat(messageReader.countMessages(FROM, TO)).isEqualTo(2);
        assertThat(messageReader.countMessages(null, TO)).isEqualTo(2);
        assertThat(messageReader.countMessages(FROM, null)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void 신고_수는_처리_상태와_상관없이_기간_안의_것을_센다() {
        assertThat(messageReader.countReports(FROM, TO)).isEqualTo(2);
        assertThat(messageReader.countReports(null, TO)).isEqualTo(2);
        assertThat(messageReader.countReports(FROM, null)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void 콘테스트_참여_수는_취소와_기간_밖을_뺀다() {
        assertThat(contestReader.countEntries(FROM, TO)).isEqualTo(2);
        assertThat(contestReader.countEntries(null, TO)).isEqualTo(2);
        assertThat(contestReader.countEntries(FROM, null)).isGreaterThanOrEqualTo(3);
    }
}
