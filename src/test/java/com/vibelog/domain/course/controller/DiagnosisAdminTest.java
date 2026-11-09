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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 진단 설정 화면 (A-09, API-ADM-41~45, #187). 진짜 SecurityConfig 로 운영자만 들어오는지까지 본다.
 * - 입력 오류는 모달을 연 채로 칸 메시지(④), 사용·중지·테스트 응답은 같은 화면 + 토스트·미리 보기(③)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DiagnosisAdminTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("UPDATE diagnosis_question SET is_active = FALSE");
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9551, '화면 트랙', '대상', '설명', 9551)");
        jdbcTemplate.update("INSERT INTO diagnosis_question (diagnosis_question_id, content, sort_order, is_active) VALUES (9551, '화면 문항', 1, TRUE)");
        jdbcTemplate.update("INSERT INTO diagnosis_option (diagnosis_option_id, diagnosis_question_id, track_id, content, score, sort_order) VALUES "
                + "(95511, 9551, 9551, '화면 보기A', 1, 1), (95512, 9551, 9551, '화면 보기B', 2, 2)");
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자는_진단_설정에_들어갈_수_없다() throws Exception {
        mockMvc.perform(get("/admin/diagnosis")).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 운영자는_문항과_보기와_테스트_응답_칸을_본다() throws Exception {
        mockMvc.perform(get("/admin/diagnosis"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/admin-diagnosis"))
                .andExpect(content().string(Matchers.containsString("화면 문항")))
                .andExpect(content().string(Matchers.containsString("화면 보기B")))
                .andExpect(content().string(Matchers.containsString("name=\"diagnosisOptionIds\"")))
                .andExpect(content().string(Matchers.containsString("href=\"/admin/diagnosis\"")));   // 왼쪽 메뉴
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수정_모달은_저장된_보기를_채워_연다() throws Exception {
        mockMvc.perform(get("/admin/diagnosis").param("questionId", "9551"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("문항 수정")))
                .andExpect(content().string(Matchers.containsString("value=\"화면 보기A\"")))
                .andExpect(content().string(Matchers.containsString("/admin/diagnosis/questions/9551/edit")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 보기가_하나뿐이면_모달을_연_채로_칸_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/diagnosis/questions").with(csrf())
                        .param("content", "새 문항").param("sortOrder", "2")
                        .param("options[0].content", "보기").param("options[0].trackId", "9551").param("options[0].score", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/admin-diagnosis"))
                .andExpect(content().string(Matchers.containsString("보기를 2개 이상 입력해 주세요.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 점수가_INT_범위를_넘으면_칸_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/diagnosis/questions").with(csrf())
                        .param("content", "새 문항").param("sortOrder", "2")
                        .param("options[0].content", "보기A").param("options[0].trackId", "9551").param("options[0].score", "2147483648")
                        .param("options[1].content", "보기B").param("options[1].trackId", "9551").param("options[1].score", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("점수는 1 이상, 2,147,483,647 이하로 입력해 주세요.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 문항을_추가하면_목록으로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/diagnosis/questions").with(csrf())
                        .param("content", "새 문항").param("sortOrder", "2")
                        .param("options[0].content", "보기A").param("options[0].trackId", "9551").param("options[0].score", "1")
                        .param("options[1].content", "보기B").param("options[1].trackId", "9551").param("options[1].score", "1")
                        .param("options[2].content", ""))
                .andExpect(redirectedUrl("/admin/diagnosis"))
                .andExpect(flash().attribute("toast", "문항을 추가했습니다"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 마지막_사용_중_문항을_중지하면_안내만_한다() throws Exception {
        mockMvc.perform(post("/admin/diagnosis/questions/9551/active").with(csrf()).param("active", "false"))
                .andExpect(redirectedUrl("/admin/diagnosis"))
                .andExpect(flash().attribute("toast", "사용 중인 문항이 하나는 있어야 합니다. 다른 문항을 먼저 다시 사용하세요."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 테스트_응답은_미리_보기를_넘긴다() throws Exception {
        mockMvc.perform(post("/admin/diagnosis/test").with(csrf()).param("diagnosisOptionIds", "95512"))
                .andExpect(redirectedUrl("/admin/diagnosis#dg-test"))
                .andExpect(flash().attributeExists("preview"));

        mockMvc.perform(post("/admin/diagnosis/test").with(csrf()))
                .andExpect(flash().attribute("toast", "사용 중인 문항마다 보기를 하나씩 골라 주세요."));
    }
}
