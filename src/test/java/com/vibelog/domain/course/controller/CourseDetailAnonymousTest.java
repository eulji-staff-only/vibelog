package com.vibelog.domain.course.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 비회원 코스 상세 (API-CRS-03, #98). 진짜 SecurityConfig 로 비회원이 열 수 있는지까지 확인한다.
 * - 공개 코스는 비회원도 열리고, 수강 시작 대신 "로그인하고 돌아오기" 버튼이 보인다 (U-11 → U-03 redirect)
 * - 비공개 코스는 비회원에게도 404 (있다고 알리지 않는다)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CourseDetailAnonymousTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9801, '상세 트랙', '대상', '설명', 9801)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9801, '상세 카테고리', 9801)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) VALUES "
                + "(9801, 9801, 9801, '공개 상세 코스', '한 줄', '소개', '대상', '목표', 7, TRUE), "
                + "(9802, 9801, 9801, '비공개 상세 코스', '한 줄', '소개', '대상', '목표', 7, FALSE)");
    }

    @Test
    void 비회원은_공개_코스_상세에서_로그인하고_돌아오는_버튼을_본다() throws Exception {
        mockMvc.perform(get("/courses/9801"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("공개 상세 코스")))
                .andExpect(content().string(Matchers.containsString("/login?redirect=/courses/9801")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("action=\"/courses/9801/enroll\""))));
    }

    @Test
    void 비공개_코스_상세는_비회원에게도_404() throws Exception {
        mockMvc.perform(get("/courses/9802"))
                .andExpect(status().isNotFound());
    }
}
