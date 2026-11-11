package com.vibelog.domain.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 운영자 회원 목록·상세 (API-ADM-37·38, FR-USR-009, #119). 넣은 데이터는 테스트가 끝나면 되돌린다(@Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminMemberTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자는_들어올_수_없다() throws Exception {
        mockMvc.perform(get("/admin/members")).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 회원_목록과_상세는_백오피스_왼쪽_메뉴_안에서_보인다() throws Exception {
        Long memberId = memberOf("틀확인회원", "frame@vibelog.test", "ACTIVE");

        for (String url : new String[]{"/admin/members", "/admin/members/" + memberId}) {
            mockMvc.perform(get(url))
                    .andExpect(content().string(allOf(containsString("vl-admin__sidebar"), containsString("href=\"/admin\""),
                            containsString("vl-admin__link is-active"), not(containsString("vl-header")))));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 닉네임이나_이메일_일부로_찾고_탈퇴_회원은_뺀다() throws Exception {
        memberOf("찾을회원가", "find-a@vibelog.test", "ACTIVE");
        memberOf("다른사람", "find-target@vibelog.test", "ACTIVE");
        memberOf("찾을회원탈퇴", null, "WITHDRAWN");

        mockMvc.perform(get("/admin/members").param("q", "찾을회원"))
                .andExpect(content().string(allOf(containsString("찾을회원가"), not(containsString("다른사람")),
                        not(containsString("찾을회원탈퇴")))));
        mockMvc.perform(get("/admin/members").param("q", "find-target"))
                .andExpect(content().string(allOf(containsString("다른사람"), not(containsString("찾을회원가")))));
        mockMvc.perform(get("/admin/members").param("q", "아무도없는검색어"))
                .andExpect(content().string(containsString("조건에 맞는 회원이 없습니다")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 검색어의_밑줄과_퍼센트는_글자_그대로_찾는다() throws Exception {
        memberOf("밑줄회원", "under_score@vibelog.test", "ACTIVE");
        memberOf("다른글자회원", "underxscore@vibelog.test", "ACTIVE");

        mockMvc.perform(get("/admin/members").param("q", "under_score"))
                .andExpect(content().string(allOf(containsString("밑줄회원"), not(containsString("다른글자회원")))));
        mockMvc.perform(get("/admin/members").param("q", "100%"))
                .andExpect(content().string(containsString("조건에 맞는 회원이 없습니다")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 상세에_수강_코스와_진행률_제출한_퀴즈와_모의고사_결과가_보인다() throws Exception {
        long memberId = memberOf("상세회원", "detail@vibelog.test", "ACTIVE");
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9301, '트랙', '대상', '설명', 9301)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9301, '카테고리', 9301)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9301, 9301, 9301, '자바로 배우는 바이브코딩', '요약', '설명', '대상', '목표', 7, TRUE)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9301, 9301, '변수와 자료형', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9302, 9301, '조건문', '소개', '목표', '본문', 30, 2, TRUE), "
                + "(9303, 9301, '반복문', '소개', '목표', '본문', 30, 3, TRUE)");
        jdbcTemplate.update("INSERT INTO enrollment (member_id, course_id, started_at) VALUES (?, 9301, NOW())", memberId);
        long enrollmentId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at, completed_at) VALUES (?, 9301, NOW(), NOW()), (?, 9302, NOW(), NULL)",
                enrollmentId, enrollmentId);
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9301, 9301, 'QUIZ', '변수 퀴즈', NULL, NULL, 10, 50, 1), (9302, 9301, 'QUIZ', '자료형 퀴즈', NULL, NULL, 10, 50, 2)");
        jdbcTemplate.update("INSERT INTO quiz_attempt (member_id, activity_id, started_at, correct_rate, is_passed, submitted_at) VALUES "
                + "(?, 9301, NOW(), 75.00, TRUE, NOW()), (?, 9302, NOW(), NULL, NULL, NULL)", memberId, memberId);
        jdbcTemplate.update("INSERT INTO exam_attempt (member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at) VALUES "
                + "(?, 9301, 1, NOW(), 85, 85.00, NOW()), (?, 9302, 1, NOW(), NULL, NULL, NULL)", memberId, memberId);

        mockMvc.perform(get("/admin/members/" + memberId))
                .andExpect(content().string(allOf(containsString("detail@vibelog.test"), containsString("자바로 배우는 바이브코딩"),
                        containsString("33%"), containsString("수강 중"), containsString("변수 퀴즈"), containsString("75%"),
                        containsString("통과"), not(containsString("자료형 퀴즈")), containsString("변수와 자료형"),
                        containsString("85점"), not(containsString("조건문")), containsString("0개"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 없는_회원은_찾을_수_없다() throws Exception {
        mockMvc.perform(get("/admin/members/999999999")).andExpect(status().isNotFound());
    }

    private long memberOf(String nickname, String email, String status) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, status) VALUES (?, 'x', ?, ?)", email, nickname, status);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
