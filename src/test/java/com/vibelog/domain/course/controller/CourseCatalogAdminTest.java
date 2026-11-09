package com.vibelog.domain.course.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 트랙·카테고리 관리 화면 (A-02·A-03, #95). 진짜 SecurityConfig 로 운영자만 들어오는지까지 본다.
 * - 이름 중복은 모달 폼을 다시 그리며 칸 메시지(④), 코스가 있어 못 지우면 목록 + 안내(③)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CourseCatalogAdminTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9961, '화면 트랙', '대상', '설명', 9961)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9961, '화면 카테고리', 9961)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9961, 9961, 9961, '화면 코스', '한 줄', '소개', '대상', '목표', 7)");
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자는_트랙_카테고리_관리에_들어갈_수_없다() throws Exception {
        mockMvc.perform(get("/admin/tracks")).andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/admin/categories")).andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/api/admin/categories/order").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseCategoryIds\": [9961]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 운영자는_트랙_목록을_본다() throws Exception {
        mockMvc.perform(get("/admin/tracks"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/admin-tracks"))
                .andExpect(content().string(Matchers.containsString("화면 트랙")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 같은_이름으로_만들면_모달을_연_채로_칸_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/tracks").with(csrf())
                        .param("name", "화면 트랙").param("target", "대상").param("description", "설명").param("sortOrder", "9962"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/admin-tracks"))
                .andExpect(content().string(Matchers.containsString("같은 이름이 이미 있습니다.")))
                .andExpect(content().string(Matchers.containsString("aria-invalid=\"true\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 빈_이름이면_칸_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/categories").with(csrf()).param("name", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("카테고리 이름을 입력해 주세요.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 트랙을_만들면_목록으로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/tracks").with(csrf())
                        .param("name", "화면 새 트랙").param("target", "대상").param("description", "설명").param("sortOrder", "9962"))
                .andExpect(redirectedUrl("/admin/tracks"))
                .andExpect(flash().attribute("toast", "트랙을 만들었습니다"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 코스가_있는_트랙_삭제는_막고_안내한다() throws Exception {
        mockMvc.perform(post("/admin/tracks/9961/delete").with(csrf()))
                .andExpect(redirectedUrl("/admin/tracks"))
                .andExpect(flash().attribute("toast", "코스가 있어 삭제할 수 없습니다. 코스를 먼저 옮겨 주세요."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 카테고리_순서가_잘못되면_400_JSON() throws Exception {
        mockMvc.perform(post("/api/admin/categories/order").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseCategoryIds\": [99999]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
