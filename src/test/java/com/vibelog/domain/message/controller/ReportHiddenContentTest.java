package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import com.vibelog.domain.community.model.dto.MyReviewItem;
import com.vibelog.domain.community.model.dto.RankingItem;
import com.vibelog.domain.community.model.dto.ReviewRequestCard;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.community.model.service.PeerReviewService;
import com.vibelog.domain.community.model.service.RecommendService;
import com.vibelog.domain.contest.model.dto.ContestCurrentView.EntryCard;
import com.vibelog.domain.contest.model.service.ContestResultService;
import com.vibelog.domain.contest.model.service.ContestService;
import com.vibelog.domain.message.model.service.MessageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
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
 * 신고 숨김 (FR-RPT-002·003, PL-RPT-002, #256). 서로 다른 3명의 처리 대기 신고나 운영자 숨김이면 숨기고, 2명이면 그대로 보인다.
 * 다른 사람에게는 목록에서 빼거나 숨김 문구, 작성자 본인에게는 내용 + 숨김 안내. 넣은 데이터는 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportHiddenContentTest {

    private static final String HIDDEN_TEXT = MessageReader.HIDDEN_TEXT;
    private static final String NOTICE = "운영 정책에 따라 숨겨졌습니다. 다른 사람에게는 보이지 않습니다";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired RecommendService recommendService;
    @Autowired PeerReviewService peerReviewService;
    @Autowired CommunityReader communityReader;
    @Autowired ContestService contestService;
    @Autowired ContestResultService contestResultService;

    private long writerId;

    @BeforeEach
    void 글쓴이를_넣는다() {
        writerId = member();
    }

    @Test
    void 두_명이_신고하면_보이고_세_명이면_목록과_공개_프로필에서_빠지고_상세는_숨김_문구다() throws Exception {
        String title = "신고글" + UUID.randomUUID().toString().substring(0, 6);
        long postId = post(writerId, title);
        long viewer = member();
        report("POST", postId, writerId, 2, "PENDING");

        mockMvc.perform(get("/community").param("q", title)).andExpect(content().string(containsString(link(postId))));
        mockMvc.perform(get("/community/{id}", postId)).andExpect(status().isOk());

        report("POST", postId, writerId, 1, "PENDING");

        mockMvc.perform(get("/community").param("q", title)).andExpect(content().string(not(containsString(link(postId)))));
        mockMvc.perform(get("/community/{id}", postId))
                .andExpect(status().isForbidden()).andExpect(content().string(containsString(HIDDEN_TEXT)));
        mockMvc.perform(get("/users/{id}", writerId).with(as(viewer))).andExpect(content().string(not(containsString(link(postId)))));
        // 작성자 본인에게는 내용 + 숨김 안내, 내 프로필에도 남는다
        mockMvc.perform(get("/community/{id}", postId).with(as(writerId)))
                .andExpect(status().isOk()).andExpect(content().string(allOf(containsString(title), containsString(NOTICE))));
        mockMvc.perform(get("/users/{id}", writerId).with(as(writerId))).andExpect(content().string(containsString(link(postId))));
    }

    @Test
    void 운영자가_숨기면_한_건이어도_숨기고_기각된_신고는_세지_않는다() {
        long hiddenPost = showcase(writerId);
        long rejectedPost = showcase(writerId);
        long recommender = member();
        jdbcTemplate.update("INSERT INTO recommendation (member_id, post_id) VALUES (?, ?), (?, ?)", recommender, hiddenPost, recommender, rejectedPost);
        report("POST", hiddenPost, writerId, 1, "HIDDEN");
        report("POST", rejectedPost, writerId, 3, "REJECTED");

        assertThat(recommendService.getWeeklyTop(100)).extracting(RankingItem::postId).contains(rejectedPost).doesNotContain(hiddenPost);
    }

    @Test
    void 숨긴_댓글은_남에게_숨김_문구_작성자에게_내용과_안내를_보여준다() throws Exception {
        long postId = post(writerId, "댓글 달린 글");
        long commenter = member();
        long commentId = insert("INSERT INTO comment (post_id, member_id, content) VALUES (?, ?, '숨길 댓글')", postId, commenter);
        report("COMMENT", commentId, commenter, 3, "PENDING");

        mockMvc.perform(get("/community/{id}", postId))
                .andExpect(content().string(allOf(containsString(HIDDEN_TEXT), not(containsString("숨길 댓글")))));
        mockMvc.perform(get("/community/{id}", postId).with(as(commenter)))
                .andExpect(content().string(allOf(containsString("숨길 댓글"), containsString(NOTICE))));
    }

    @Test
    void 숨긴_작품은_리뷰_요청_목록에서_빠지고_내가_쓴_리뷰의_제목은_숨김_문구다() {
        long portfolioId = portfolio(writerId);
        long requestId = insert("INSERT INTO review_request (portfolio_id, requested_at) VALUES (?, NOW())", portfolioId);
        long reviewer = member();
        insert("INSERT INTO peer_review (review_request_id, reviewer_member_id) VALUES (?, ?)", requestId, reviewer);
        long totalBefore = peerReviewService.getOpenRequests(1).getTotalElements();
        report("PORTFOLIO", portfolioId, writerId, 3, "PENDING");

        // 페이지를 자르기 전에 빼므로 전체 건수에서도 빠진다
        var requests = peerReviewService.getOpenRequests(1);
        assertThat(requests.getContent()).extracting(ReviewRequestCard::portfolioId).doesNotContain(portfolioId);
        assertThat(requests.getTotalElements()).isEqualTo(totalBefore - 1);
        assertThat(peerReviewService.getMyReviews(reviewer, 1).getContent())
                .singleElement().extracting(MyReviewItem::portfolioTitle).isEqualTo(HIDDEN_TEXT);
    }

    @Test
    void 숨긴_리뷰는_작품_주인에게_숨기고_쓴_사람에게는_내용과_안내를_붙인다() {
        long owner = member();
        long portfolioId = portfolio(owner);
        long requestId = insert("INSERT INTO review_request (portfolio_id, requested_at) VALUES (?, NOW())", portfolioId);
        long reviewId = insert("INSERT INTO peer_review (review_request_id, reviewer_member_id) VALUES (?, ?)", requestId, writerId);
        insert("INSERT INTO review_comment (peer_review_id, content) VALUES (?, '숨길 리뷰')", reviewId);
        report("REVIEW", reviewId, writerId, 3, "PENDING");

        assertThat(communityReader.findReviewsOfPortfolio(portfolioId, owner)).singleElement().satisfies(review -> {
            assertThat(review.hidden()).isTrue();
            assertThat(review.comments()).isEmpty();
        });
        assertThat(communityReader.findReviewsOfPortfolio(portfolioId, writerId)).singleElement().satisfies(review -> {
            assertThat(review.hidden()).isFalse();
            assertThat(review.hiddenNotice()).isTrue();
            assertThat(review.comments()).isNotEmpty();
        });
    }

    @Test
    void 운영자가_숨긴_쪽지는_받은_사람에게_숨김_문구_보낸_사람에게_내용과_안내를_보여준다() throws Exception {
        long receiver = member();
        long messageId = insert("INSERT INTO message (sender_member_id, receiver_member_id, content) VALUES (?, ?, '숨길 쪽지')", writerId, receiver);
        report("MESSAGE", messageId, writerId, 1, "HIDDEN");

        mockMvc.perform(get("/messages").param("messageId", String.valueOf(messageId)).with(as(receiver)))
                .andExpect(content().string(allOf(containsString(HIDDEN_TEXT), not(containsString("숨길 쪽지")),
                        not(containsString("data-report-type=\"MESSAGE\"")))));
        mockMvc.perform(get("/messages").param("box", "sent").param("messageId", String.valueOf(messageId)).with(as(writerId)))
                .andExpect(content().string(allOf(containsString("숨길 쪽지"), containsString(NOTICE))));
    }

    @Test
    void 숨긴_수상작은_제목_대신_숨김_문구이고_투표_보드에서는_남에게만_빠진다() {
        long portfolioId = portfolio(writerId);
        LocalDateTime now = LocalDateTime.now();
        long pastContest = contest("2000-01", now.minusDays(40), now.minusDays(35), now.minusDays(35), now.minusDays(30));
        long pastEntry = entry(pastContest, writerId, portfolioId);
        jdbcTemplate.update("INSERT INTO contest_award (contest_id, contest_entry_id, award_rank) VALUES (?, ?, 1)", pastContest, pastEntry);
        report("PORTFOLIO", portfolioId, writerId, 1, "HIDDEN");

        assertThat(contestResultService.getResult(pastContest).awards()).singleElement().satisfies(award -> {
            assertThat(award.reportHidden()).isTrue();
            assertThat(award.visible()).isFalse();
        });

        long current = currentContest(now.minusDays(10), now.minusDays(5), now.minusDays(5), now.plusDays(5));
        long other = member();
        long otherPortfolio = portfolio(other);
        entry(current, writerId, portfolioId);
        entry(current, other, otherPortfolio);

        assertThat(contestService.getCurrent(other).orElseThrow().entries()).extracting(EntryCard::portfolioId)
                .contains(otherPortfolio).doesNotContain(portfolioId);
        assertThat(contestService.getCurrent(writerId).orElseThrow().entries()).extracting(EntryCard::portfolioId).contains(portfolioId);
    }

    @Test
    void 숨긴_포트폴리오는_남에게_숨김_문구_작성자에게_내용과_안내이고_같은_코스_작품에서_빠진다() throws Exception {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9701, '트랙', '대상', '설명', 9701)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9701, '카테고리', 9701)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9701, 9701, 9701, '신고 코스', '요약', '설명', '대상', '목표', 7, TRUE)");
        long viewer = member();
        long hidden = coursePortfolio(writerId, "숨길 코스 작품");
        long shown = coursePortfolio(viewer, "보이는 코스 작품");
        report("PORTFOLIO", hidden, writerId, 3, "PENDING");

        mockMvc.perform(get("/portfolios/{id}", hidden).with(as(viewer)))
                .andExpect(status().isForbidden()).andExpect(content().string(allOf(containsString(HIDDEN_TEXT), not(containsString("숨길 코스 작품")))));
        mockMvc.perform(get("/portfolios/{id}", hidden).with(as(writerId)))
                .andExpect(status().isOk()).andExpect(content().string(allOf(containsString("숨길 코스 작품"), containsString(NOTICE))));
        mockMvc.perform(get("/courses/9701/portfolios").with(as(viewer)))
                .andExpect(content().string(allOf(containsString("/portfolios/" + shown + "\""), not(containsString("/portfolios/" + hidden + "\"")))));
        // 작성자가 비공개로 바꿔도 다른 회원에게는 숨김 문구가 먼저다 (영구 정지 숨김과 같은 순서)
        jdbcTemplate.update("UPDATE portfolio SET is_public = FALSE WHERE portfolio_id = ?", hidden);
        mockMvc.perform(get("/portfolios/{id}", hidden).with(as(viewer)))
                .andExpect(content().string(allOf(containsString(HIDDEN_TEXT), not(containsString("비공개된 작품입니다")))));
    }

    @Test
    void 포트폴리오_상세에는_남의_작품과_남의_리뷰에만_신고_버튼이_보인다() throws Exception {
        long portfolioId = portfolio(writerId);
        long requestId = insert("INSERT INTO review_request (portfolio_id, requested_at) VALUES (?, NOW())", portfolioId);
        long reviewer = member();
        long reviewId = insert("INSERT INTO peer_review (review_request_id, reviewer_member_id) VALUES (?, ?)", requestId, reviewer);
        insert("INSERT INTO review_comment (peer_review_id, content) VALUES (?, '리뷰 내용')", reviewId);
        String portfolioButton = "data-report-type=\"PORTFOLIO\"";
        String reviewButton = "data-report-type=\"REVIEW\"";

        mockMvc.perform(get("/portfolios/{id}", portfolioId).with(as(member())))
                .andExpect(content().string(allOf(containsString(portfolioButton), containsString(reviewButton), containsString("id=\"report-modal\""))));
        mockMvc.perform(get("/portfolios/{id}", portfolioId).with(as(writerId)))
                .andExpect(content().string(allOf(not(containsString(portfolioButton)), containsString(reviewButton))));
        mockMvc.perform(get("/portfolios/{id}", portfolioId).with(as(reviewer)))
                .andExpect(content().string(allOf(containsString(portfolioButton), not(containsString(reviewButton)))));
    }

    private long coursePortfolio(long memberId, String title) {
        return insert("INSERT INTO portfolio (member_id, course_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public) "
                + "VALUES (?, 9701, 'COURSE', ?, 'https://example.com', '요약', '회고', 'Claude', TRUE)", memberId, title);
    }

    /* 서로 다른 신고자 count 명이 status 로 신고한다 */
    private void report(String targetType, long targetId, long authorId, int count, String status) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update("INSERT INTO report (reporter_member_id, target_type, target_id, target_member_id, reason, status, handled_at)"
                    + " VALUES (?, ?, ?, ?, 'SPAM', ?, ?)", member(), targetType, targetId, authorId, status,
                    "PENDING".equals(status) ? null : LocalDateTime.now());
        }
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
        return insert("INSERT INTO contest (contest_month, topic, description, entry_starts_at, entry_ends_at, vote_starts_at, vote_ends_at)"
                + " VALUES (?, '주제', '설명', ?, ?, ?, ?)", month, entryStarts, entryEnds, voteStarts, voteEnds);
    }

    private long entry(long contestId, long memberId, long portfolioId) {
        return insert("INSERT INTO contest_entry (contest_id, member_id, portfolio_id) VALUES (?, ?, ?)", contestId, memberId, portfolioId);
    }

    private long showcase(long memberId) {
        return insert("INSERT INTO post (member_id, category, title, body, work_url) VALUES (?, 'SHOWCASE', '작품', '본문', 'https://example.com')",
                memberId);
    }

    private long post(long memberId, String title) {
        return insert("INSERT INTO post (member_id, category, title, body) VALUES (?, 'FREE', ?, '본문')", memberId, title);
    }

    private long portfolio(long memberId) {
        return insert("INSERT INTO portfolio (member_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public) "
                + "VALUES (?, 'PERSONAL', '신고된 작품', 'https://example.com', '요약', '회고', 'Claude', TRUE)", memberId);
    }

    private long member() {
        return insert("INSERT INTO member (email, password_hash, nickname, role, email_verified_at) VALUES (?, 'x', '신고회원', 'LEARNER', NOW())",
                "report-hide-" + UUID.randomUUID() + "@vibelog.test");
    }

    private long insert(String sql, Object... args) {
        jdbcTemplate.update(sql, args);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private RequestPostProcessor as(long memberId) {
        String email = jdbcTemplate.queryForObject("SELECT email FROM member WHERE member_id = ?", String.class, memberId);
        return user(memberLoginService.loadUserByUsername(email));
    }

    private static String link(long postId) {
        return "href=\"/community/" + postId + "\"";
    }
}
