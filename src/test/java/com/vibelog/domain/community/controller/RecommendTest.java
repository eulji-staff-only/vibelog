package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import com.vibelog.domain.community.model.dto.RankingItem;
import com.vibelog.domain.community.model.service.CommunityReader;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 추천·추천 랭킹 (API-CMT-11·12, FR-CMT-005·006, PL-CMT-001~004, #179).
 * 회원·글·추천은 테스트마다 새로 넣고 끝나면 롤백된다. 개발 DB 에 다른 추천이 있어도 깨지지 않게 랭킹은 내 글끼리의 순서만 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecommendTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemberLoginService memberLoginService;
    @Autowired CommunityReader communityReader;

    private String writer;
    private long writerId;
    private String reader;

    @BeforeEach
    void 글쓴이와_읽는_사람을_넣는다() {
        writer = member();
        writerId = idOf(writer);
        reader = member();
    }

    @Test
    void 작품_공유_글을_추천하고_다시_누르면_취소되며_추가할_때만_알린다() throws Exception {
        long postId = postOf(writerId, "SHOWCASE", "내 작품", null);

        recommend(reader, postId).andExpect(status().isOk())
                .andExpect(jsonPath("$.recommended").value(true)).andExpect(jsonPath("$.recommendCount").value(1));
        recommend(reader, postId)
                .andExpect(jsonPath("$.recommended").value(false)).andExpect(jsonPath("$.recommendCount").value(0));

        assertThat(jdbcTemplate.queryForList("SELECT content FROM notification WHERE member_id = ? AND notification_type = 'RECOMMEND'",
                String.class, writerId)).containsExactly("내 글 「내 작품」이 추천을 받았습니다");
    }

    @Test
    void 작품_공유_글이_아니거나_내_글이거나_지운_글이면_추천할_수_없다() throws Exception {
        long free = postOf(writerId, "FREE", "자유 글", null);
        long mine = postOf(writerId, "SHOWCASE", "내 작품", null);
        long deleted = postOf(writerId, "SHOWCASE", "지운 작품", LocalDateTime.now());

        recommend(reader, free).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_SHOWCASE"));
        recommend(writer, mine).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SELF_RECOMMEND"));
        recommend(reader, deleted).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        mockMvc.perform(post("/api/posts/{id}/recommend", mine).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void 랭킹은_이번_주_추천_수_순이고_동점이면_이번_주_첫_추천이_빠른_글이_위다() throws Exception {
        LocalDateTime monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        long two = postOf(writerId, "SHOWCASE", "랭킹 둘", null);
        long tieLate = postOf(writerId, "SHOWCASE", "랭킹 하나 늦게", null);
        long tieEarly = postOf(writerId, "SHOWCASE", "랭킹 하나 먼저", null);
        long lastWeekOnly = postOf(writerId, "SHOWCASE", "지난주만", null);
        long deleted = postOf(writerId, "SHOWCASE", "지운 글", LocalDateTime.now());
        recommendAt(two, monday.plusMinutes(30));
        recommendAt(two, monday.plusMinutes(40));
        recommendAt(tieLate, monday.plusMinutes(20));
        recommendAt(tieEarly, monday.plusMinutes(10));
        recommendAt(lastWeekOnly, monday.minusDays(1));
        recommendAt(lastWeekOnly, monday.minusDays(2));
        recommendAt(deleted, monday.plusMinutes(5));

        List<Long> ids = communityReader.findWeeklyTop(10).stream().map(RankingItem::postId).toList();

        assertThat(ids).containsSubsequence(two, tieEarly, tieLate).doesNotContain(lastWeekOnly, deleted);
        mockMvc.perform(get("/community/ranking"))
                .andExpect(content().string(allOf(containsString("랭킹 둘"), containsString("추천 2"), not(containsString("지난주만")))));
    }

    @Test
    void 상세에는_작품_공유_글만_추천_수가_보이고_버튼은_다른_회원에게만_있고_비회원은_로그인으로_간다() throws Exception {
        long showcase = postOf(writerId, "SHOWCASE", "작품", null);
        long free = postOf(writerId, "FREE", "자유", null);
        recommendAt(showcase, LocalDateTime.now());

        String api = "/api/posts/" + showcase + "/recommend";
        mockMvc.perform(get("/community/{id}", showcase).with(as(reader)))
                .andExpect(content().string(allOf(containsString(api), containsString("추천 1"))));
        mockMvc.perform(get("/community/{id}", showcase).with(as(writer)))
                .andExpect(content().string(allOf(not(containsString(api)), containsString("추천 1"))));
        mockMvc.perform(get("/community/{id}", free).with(as(reader)))
                .andExpect(content().string(not(containsString("cm-recommend"))));
        // U-22 예외: 비회원이 추천을 누르면 U-03 로그인, 로그인 뒤 이 글로 돌아온다 (#363)
        mockMvc.perform(get("/community/{id}", showcase))
                .andExpect(content().string(allOf(not(containsString(api)),
                        containsString("href=\"/login?redirect=/community/" + showcase + "\">추천</a>"))));
    }

    private ResultActions recommend(String email, long postId) throws Exception {
        return mockMvc.perform(post("/api/posts/{id}/recommend", postId).with(as(email)).with(csrf()));
    }

    /* 다른 회원 한 명이 그 시각에 추천한 것으로 넣는다 */
    private void recommendAt(long postId, LocalDateTime at) {
        jdbcTemplate.update("INSERT INTO recommendation (member_id, post_id, created_at) VALUES (?, ?, ?)", idOf(member()), postId, at);
    }

    private long postOf(long memberId, String category, String title, LocalDateTime deletedAt) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body, work_url, deleted_at) VALUES (?, ?, ?, '본문', ?, ?)",
                memberId, category, title, "SHOWCASE".equals(category) ? "https://example.com/work" : null, deletedAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String member() {
        String email = "recommend-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '추천회원', NOW())", email);
        return email;
    }

    private long idOf(String email) {
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, email);
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }
}
