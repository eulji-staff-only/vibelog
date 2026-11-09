package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import com.vibelog.domain.community.model.dto.ReviewView;
import com.vibelog.domain.community.model.policy.ReviewRequestCloser;
import com.vibelog.domain.community.model.service.CommunityReader;
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
import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 동료 리뷰 (API-REV-01~07, FR-REV-001~005, PL-REV-001~003, 계약 ⑭, #182·#183).
 * 회원·포트폴리오·요청·리뷰는 테스트마다 새로 넣고 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PeerReviewTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired CommunityReader communityReader;
    @Autowired ReviewRequestCloser reviewRequestCloser;
    @Autowired EntityManager entityManager;

    private String owner;
    private long ownerId;
    private String reviewer;
    private long reviewerId;

    @BeforeEach
    void 작품_주인과_리뷰어를_넣는다() {
        owner = member(true);
        ownerId = idOf(owner);
        reviewer = member(true);
        reviewerId = idOf(reviewer);
    }

    @Test
    void 공개_작품에만_요청하고_작품당_열린_요청은_하나_내_열린_요청은_3개까지() throws Exception {
        long p1 = portfolio(ownerId, null, true, "작품1");
        long p2 = portfolio(ownerId, null, true, "작품2");
        long p3 = portfolio(ownerId, null, true, "작품3");
        long p4 = portfolio(ownerId, null, true, "작품4");
        long hidden = portfolio(ownerId, null, false, "비공개 작품");
        long others = portfolio(reviewerId, null, true, "남의 작품");

        request(owner, p1).andExpect(redirectedUrl("/portfolios/" + p1)).andExpect(flash().attribute("toast", "리뷰를 요청했습니다"));
        request(owner, p1).andExpect(flash().attribute("toast", "이 작품은 이미 리뷰를 요청했습니다."));
        request(owner, hidden).andExpect(flash().attribute("toast", "비공개된 작품입니다."));
        request(owner, others).andExpect(flash().attribute("toast", "본인만 할 수 있습니다."));
        request(owner, p2);
        request(owner, p3);
        request(owner, p4).andExpect(flash().attribute("toast", "리뷰 요청은 동시에 3개까지 할 수 있습니다. 지난 요청을 마감한 뒤 다시 요청해 주세요."));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM review_request WHERE portfolio_id IN (?, ?, ?, ?, ?)",
                Long.class, p1, p2, p3, p4, hidden)).isEqualTo(3);
    }

    @Test
    void 리뷰를_쓰면_알림이_가고_내_리뷰에서_고치고_지우고_다시_쓰면_되살린다() throws Exception {
        long portfolioId = portfolio(ownerId, null, true, "내 첫 작품");
        long requestId = openRequest(portfolioId, LocalDateTime.now());

        mockMvc.perform(get("/reviews").param("reviewRequestId", String.valueOf(requestId)).with(as(reviewer)))
                .andExpect(content().string(allOf(containsString("리뷰 쓰기"), containsString("내 첫 작품"))));
        write(reviewer, requestId, null, "좋은 작품이에요").andExpect(redirectedUrl("/portfolios/" + portfolioId))
                .andExpect(flash().attribute("toast", "리뷰를 남겼습니다"));
        long reviewId = jdbcTemplate.queryForObject("SELECT peer_review_id FROM peer_review WHERE review_request_id = ?", Long.class, requestId);
        assertThat(jdbcTemplate.queryForList("SELECT content FROM notification WHERE member_id = ? AND notification_type = 'REVIEW'",
                String.class, ownerId)).containsExactly("내 작품 「내 첫 작품」에 리뷰가 달렸습니다");

        write(reviewer, requestId, null, "한 번 더").andExpect(redirectedUrl("/reviews?reviewRequestId=" + requestId))
                .andExpect(flash().attribute("toast", "이미 리뷰를 남겼습니다. 내 리뷰를 수정해 주세요."));
        mockMvc.perform(get("/reviews").with(as(reviewer))).andExpect(content().string(allOf(containsString("내가 쓴 리뷰"),
                containsString("내 첫 작품"))));
        mockMvc.perform(form(post("/peer-reviews/{id}/edit", reviewId), reviewer, null, "고친 리뷰"))
                .andExpect(flash().attribute("toast", "리뷰를 수정했습니다"));
        assertThat(commentsOf(reviewId)).containsExactly("고친 리뷰");

        mockMvc.perform(post("/peer-reviews/{id}/delete", reviewId).with(as(reviewer)).with(csrf()))
                .andExpect(flash().attribute("toast", "리뷰를 삭제했습니다"));
        write(reviewer, requestId, null, "다시 쓴 리뷰").andExpect(flash().attribute("toast", "리뷰를 남겼습니다"));
        entityManager.flush();
        assertThat(jdbcTemplate.queryForList("SELECT peer_review_id FROM peer_review WHERE review_request_id = ? AND deleted_at IS NULL",
                Long.class, requestId)).containsExactly(reviewId);
        assertThat(commentsOf(reviewId)).containsExactly("다시 쓴 리뷰");
    }

    @Test
    void 평가_기준이_있으면_기준_묶음이_정확히_같아야_하고_기준별로_저장한다() throws Exception {
        long[] criteria = courseWithCriteria();
        long portfolioId = portfolio(ownerId, criteria[0], true, "코스 작품");
        long requestId = openRequest(portfolioId, LocalDateTime.now());

        mockMvc.perform(get("/reviews").param("reviewRequestId", String.valueOf(requestId)).with(as(reviewer)))
                .andExpect(content().string(allOf(containsString("기능 완성도"), containsString("코드 읽기 쉬움"), not(containsString("안 쓰는 기준")))));
        mockMvc.perform(post("/review-requests/{id}/reviews", requestId).with(as(reviewer)).with(csrf())
                        .param("comments[0].reviewCriterionId", String.valueOf(criteria[1])).param("comments[0].content", "하나만"))
                .andExpect(view().name("community/reviews")).andExpect(model().attributeHasErrors("reviewForm"));
        mockMvc.perform(post("/review-requests/{id}/reviews", requestId).with(as(reviewer)).with(csrf())
                        .param("comments[0].reviewCriterionId", String.valueOf(criteria[1])).param("comments[0].content", "같은 기준")
                        .param("comments[1].reviewCriterionId", String.valueOf(criteria[1])).param("comments[1].content", "두 번"))
                .andExpect(model().attributeHasErrors("reviewForm"));
        mockMvc.perform(post("/review-requests/{id}/reviews", requestId).with(as(reviewer)).with(csrf())
                        .param("comments[0].reviewCriterionId", String.valueOf(criteria[1])).param("comments[0].content", "잘 돌아가요")
                        .param("comments[1].reviewCriterionId", String.valueOf(criteria[2])).param("comments[1].content", "읽기 쉬워요"))
                .andExpect(flash().attribute("toast", "리뷰를 남겼습니다"));

        List<ReviewView> reviews = communityReader.findReviewsOfPortfolio(portfolioId, reviewerId);
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).mine()).isTrue();
        assertThat(reviews.get(0).comments()).extracting(ReviewView.Comment::criterionName, ReviewView.Comment::content)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("기능 완성도", "잘 돌아가요"),
                        org.assertj.core.groups.Tuple.tuple("코드 읽기 쉬움", "읽기 쉬워요"));
    }

    @Test
    void 쓰기_검사에_걸리면_같은_폼에_입력값과_메시지를_보여준다() throws Exception {
        long requestId = openRequest(portfolio(ownerId, null, true, "작품"), LocalDateTime.now());
        jdbcTemplate.update("INSERT INTO banned_word (word) VALUES (?)", "리뷰금칙");

        write(reviewer, requestId, null, " ").andExpect(view().name("community/reviews"))
                .andExpect(model().attributeHasFieldErrors("reviewForm", "comments[0].content"));
        write(reviewer, requestId, null, "리뷰금칙 들어감").andExpect(content().string(allOf(
                containsString("사용할 수 없는 단어가 들어 있습니다."), containsString("리뷰금칙 들어감"))));
        write(member(false), requestId, null, "인증 전").andExpect(content().string(containsString("이메일 인증을 마치면")));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM peer_review WHERE review_request_id = ?", Long.class, requestId)).isZero();
    }

    @Test
    void 내_작품_마감된_요청_14일_지난_요청_없는_요청은_폼을_열지_않는다() throws Exception {
        long portfolioId = portfolio(ownerId, null, true, "작품");
        long open = openRequest(portfolioId, LocalDateTime.now());
        long old = openRequest(portfolio(ownerId, null, true, "오래된"), LocalDateTime.now().minusDays(15));

        openForm(owner, open).andExpect(redirectedUrl("/reviews")).andExpect(flash().attribute("toast", "내 작품에는 리뷰를 쓸 수 없습니다."));
        openForm(reviewer, old).andExpect(flash().attribute("toast", "마감된 리뷰 요청입니다."));
        openForm(reviewer, 999_999_999L).andExpect(flash().attribute("toast", "리뷰 요청을 찾을 수 없습니다."));
        write(owner, open, null, "내 작품").andExpect(flash().attribute("toast", "내 작품에는 리뷰를 쓸 수 없습니다."));
    }

    @Test
    void 마감과_도움됨은_요청자만_한다() throws Exception {
        long portfolioId = portfolio(ownerId, null, true, "작품");
        long requestId = openRequest(portfolioId, LocalDateTime.now());
        write(reviewer, requestId, null, "리뷰");
        long reviewId = jdbcTemplate.queryForObject("SELECT peer_review_id FROM peer_review WHERE review_request_id = ?", Long.class, requestId);

        mockMvc.perform(post("/api/reviews/{id}/helpful", reviewId).with(as(reviewer)).with(csrf()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_OWNER"));
        mockMvc.perform(post("/api/reviews/{id}/helpful", reviewId).with(as(owner)).with(csrf())).andExpect(jsonPath("$.helpful").value(true));
        mockMvc.perform(post("/api/reviews/{id}/helpful", reviewId).with(as(owner)).with(csrf())).andExpect(jsonPath("$.helpful").value(false));

        close(reviewer, requestId).andExpect(flash().attribute("toast", "본인만 할 수 있습니다."));
        close(owner, requestId).andExpect(redirectedUrl("/portfolios/" + portfolioId)).andExpect(flash().attribute("toast", "리뷰 요청을 마감했습니다"));
        close(owner, requestId).andExpect(flash().attribute("toast", "마감된 리뷰 요청입니다."));
    }

    @Test
    void 마감돼도_내_리뷰는_고치고_작품이_비공개면_리뷰_목록으로_간다() throws Exception {
        long portfolioId = portfolio(ownerId, null, true, "작품");
        long requestId = openRequest(portfolioId, LocalDateTime.now());
        write(reviewer, requestId, null, "리뷰");
        long reviewId = jdbcTemplate.queryForObject("SELECT peer_review_id FROM peer_review WHERE review_request_id = ?", Long.class, requestId);
        jdbcTemplate.update("UPDATE review_request SET closed_at = NOW() WHERE review_request_id = ?", requestId);

        openForm(reviewer, requestId).andExpect(content().string(containsString("리뷰 수정")));
        entityManager.flush();
        jdbcTemplate.update("UPDATE portfolio SET is_public = FALSE WHERE portfolio_id = ?", portfolioId);
        entityManager.clear(); // 한 트랜잭션 안이라, 앞 요청이 읽어 둔 포트폴리오를 버려야 바뀐 값이 보인다
        mockMvc.perform(form(post("/peer-reviews/{id}/edit", reviewId), reviewer, null, "비공개 뒤 수정")).andExpect(redirectedUrl("/reviews"));
        mockMvc.perform(get("/reviews").with(as(reviewer))).andExpect(content().string(containsString("비공개된 작품")));
        mockMvc.perform(post("/peer-reviews/{id}/delete", reviewId).with(as(owner)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void 목록에는_열린_공개_작품만_나오고_Reader_값과_계약14가_맞다() throws Exception {
        long shown = portfolio(ownerId, null, true, "보이는 작품");
        long shownRequest = openRequest(shown, LocalDateTime.now());
        long privatePortfolio = portfolio(ownerId, null, true, "곧 비공개");
        openRequest(privatePortfolio, LocalDateTime.now());
        jdbcTemplate.update("UPDATE portfolio SET is_public = FALSE WHERE portfolio_id = ?", privatePortfolio);
        write(reviewer, shownRequest, null, "리뷰");

        mockMvc.perform(get("/reviews").with(as(reviewer)))
                .andExpect(content().string(allOf(containsString("보이는 작품"), containsString("리뷰 1"), not(containsString("곧 비공개")))));
        assertThat(communityReader.findOpenReviewRequestId(shown)).contains(shownRequest);
        assertThat(communityReader.findPortfolioIdsWithOpenRequest(List.of(shown, privatePortfolio))).contains(shown);
        long reviewId = jdbcTemplate.queryForObject("SELECT peer_review_id FROM peer_review WHERE review_request_id = ?", Long.class, shownRequest);
        assertThat(communityReader.findPortfolioIdOfReview(reviewId)).contains(shown);

        reviewRequestCloser.closeOpenRequests(shown);
        entityManager.flush();
        assertThat(communityReader.findOpenReviewRequestId(shown)).isEmpty();
    }

    private ResultActions request(String email, long portfolioId) throws Exception {
        return mockMvc.perform(post("/portfolios/{id}/review-requests", portfolioId).with(as(email)).with(csrf()));
    }

    private ResultActions close(String email, long requestId) throws Exception {
        return mockMvc.perform(post("/review-requests/{id}/close", requestId).with(as(email)).with(csrf()));
    }

    private ResultActions openForm(String email, long requestId) throws Exception {
        return mockMvc.perform(get("/reviews").param("reviewRequestId", String.valueOf(requestId)).with(as(email)));
    }

    private ResultActions write(String email, long requestId, Long criterionId, String text) throws Exception {
        return mockMvc.perform(form(post("/review-requests/{id}/reviews", requestId), email, criterionId, text));
    }

    private MockHttpServletRequestBuilder form(MockHttpServletRequestBuilder request, String email, Long criterionId, String text) {
        request.with(as(email)).with(csrf()).param("comments[0].content", text);
        if (criterionId != null) {
            request.param("comments[0].reviewCriterionId", String.valueOf(criterionId));
        }
        return request;
    }

    private List<String> commentsOf(long reviewId) {
        entityManager.flush();
        return jdbcTemplate.queryForList("SELECT content FROM review_comment WHERE peer_review_id = ?", String.class, reviewId);
    }

    private long openRequest(long portfolioId, LocalDateTime requestedAt) {
        jdbcTemplate.update("INSERT INTO review_request (portfolio_id, requested_at) VALUES (?, ?)", portfolioId, requestedAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /* courseId 가 null 이면 개인 작품 */
    private long portfolio(long memberId, Long courseId, boolean isPublic, String title) {
        jdbcTemplate.update("INSERT INTO portfolio (member_id, course_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public) "
                        + "VALUES (?, ?, ?, ?, 'https://example.com', '한 줄 요약', '회고', 'Claude', ?)",
                memberId, courseId, courseId == null ? "PERSONAL" : "COURSE", title, isPublic);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /* [courseId, 기준1, 기준2]. 안 쓰는 기준 1개도 넣는다 */
    private long[] courseWithCriteria() {
        jdbcTemplate.update("INSERT INTO track (name, target, description, sort_order) VALUES ('리뷰 트랙', '대상', '설명', 9501)");
        long trackId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO course_category (name, sort_order) VALUES ('리뷰 분류', 9501)");
        long categoryId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO course (track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (?, ?, '리뷰 코스', '요약', '설명', '대상', '목표', 7)", trackId, categoryId);
        long courseId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO portfolio_assignment (course_id, topic, goal, agent_guide) VALUES (?, '주제', '목표', '안내')", courseId);
        long assignmentId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long first = criterion(assignmentId, "기능 완성도", 1, true);
        long second = criterion(assignmentId, "코드 읽기 쉬움", 2, true);
        criterion(assignmentId, "안 쓰는 기준", 3, false);
        return new long[]{courseId, first, second};
    }

    private long criterion(long assignmentId, String name, int order, boolean active) {
        jdbcTemplate.update("INSERT INTO review_criterion (portfolio_assignment_id, name, sort_order, is_active) VALUES (?, ?, ?, ?)",
                assignmentId, name, order, active);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String member(boolean verified) {
        String email = "review-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '리뷰회원', ?)",
                email, verified ? LocalDateTime.now() : null);
        return email;
    }

    private long idOf(String email) {
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, email);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
