package com.vibelog.domain.course.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 코스 목록·만들기·상세 화면 (A-04·A-05, #96). 진짜 SecurityConfig 로 운영자만 들어오는지까지 본다.
 * - 입력 오류는 같은 폼에 칸 메시지(④), 없는 코스는 오류 화면(404)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CourseAdminTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9981, '코화 트랙', '대상', '설명', 9981)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9981, '코화 카테고리', 9981)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9981, 9981, 9981, '코화 코스', '한 줄', '소개', '대상', '목표', 7)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order) "
                + "VALUES (9981, 9981, '코화 수업', '소개', '목표', '본문', 10, 1)");
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자는_코스_관리에_들어갈_수_없다() throws Exception {
        mockMvc.perform(get("/admin/courses")).andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/admin/courses/9981")).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 운영자는_코스_목록을_트랙으로_걸러_본다() throws Exception {
        mockMvc.perform(get("/admin/courses").param("track", "9981"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/admin-courses"))
                .andExpect(content().string(Matchers.containsString("코화 코스")))
                .andExpect(content().string(Matchers.containsString("비공개")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 상세는_코스_정보_수업_과제_상태를_보여준다() throws Exception {
        mockMvc.perform(get("/admin/courses/9981"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/admin-course"))
                .andExpect(content().string(Matchers.containsString("코화 수업")))
                .andExpect(content().string(Matchers.containsString("과제 없음")))
                .andExpect(content().string(Matchers.containsString("/admin/courses/9981/lessons/order")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 없는_코스는_오류_화면() throws Exception {
        mockMvc.perform(get("/admin/courses/9989")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 빈_칸이_있으면_같은_폼에_칸_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/courses").with(csrf())
                        .param("trackId", "9981").param("courseCategoryId", "9981").param("title", " ")
                        .param("summary", "한 줄").param("description", "소개").param("target", "대상").param("goal", "목표"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/admin-course"))
                .andExpect(content().string(Matchers.containsString("코스 이름을 입력해 주세요.")))
                .andExpect(content().string(Matchers.containsString("플랜 일수를 입력해 주세요.")))
                .andExpect(content().string(Matchers.containsString("aria-invalid=\"true\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 없는_트랙을_보내면_트랙_칸에_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/courses").with(csrf())
                        .param("trackId", "9989").param("courseCategoryId", "9981").param("title", "코화 새 코스")
                        .param("summary", "한 줄").param("description", "소개").param("target", "대상").param("goal", "목표")
                        .param("planDays", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("트랙을 찾을 수 없습니다.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 코스를_만들면_상세로_간다() throws Exception {
        mockMvc.perform(post("/admin/courses").with(csrf())
                        .param("trackId", "9981").param("courseCategoryId", "9981").param("title", "코화 새 코스")
                        .param("summary", "한 줄").param("description", "소개").param("target", "대상").param("goal", "목표")
                        .param("planDays", "7"))
                .andExpect(redirectedUrlPattern("/admin/courses/*"))
                .andExpect(flash().attribute("toast", "코스를 만들었습니다. 비공개로 저장했습니다"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 코스를_고치면_상세로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/courses/9981/edit").with(csrf())
                        .param("trackId", "9981").param("courseCategoryId", "9981").param("title", "코화 고친 코스")
                        .param("summary", "한 줄").param("description", "소개").param("target", "대상").param("goal", "목표")
                        .param("planDays", "10"))
                .andExpect(redirectedUrl("/admin/courses/9981"))
                .andExpect(flash().attribute("toast", "코스를 수정했습니다"));
    }
}
