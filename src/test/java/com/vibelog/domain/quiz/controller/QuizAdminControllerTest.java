package com.vibelog.domain.quiz.controller;

import org.hamcrest.Matchers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 퀴즈 문항 등록 화면·요청 (A-06 활동 탭, API-ADM-26~28, #102). 진짜 SecurityConfig 로 운영자만 되는지까지 본다.
 * - 입력이 틀리면 A-06 으로 돌아가 그 모달을 열린 채로 칸 메시지(④), 맞으면 A-06 으로 돌아간다
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuizAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9781, '문항화면 트랙', '대상', '설명', 9781)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9781, '문항화면 카테고리', 9781)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9781, 9781, 9781, '문항화면 코스', '한 줄', '소개', '대상', '목표', 7)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order) "
                + "VALUES (9781, 9781, '문항화면 수업', '소개', '목표', '본문', 30, 1)");
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) "
                + "VALUES (9781, 9781, 'QUIZ', '문항화면 퀴즈', NULL, NULL, 10, 50, 1)");
        jdbcTemplate.update("INSERT INTO concept_tag (concept_tag_id, name) VALUES (9781, '문항화면 태그')");
        jdbcTemplate.update("INSERT INTO quiz_question (quiz_question_id, activity_id, concept_tag_id, display_type, content, code_snippet, explanation, sort_order) "
                + "VALUES (97811, 9781, 9781, 'OUTPUT_PREDICT', '저장된 문항', 'print(1)', '해설', 1)");
        jdbcTemplate.update("INSERT INTO quiz_choice (quiz_question_id, content, is_correct, sort_order) VALUES "
                + "(97811, '1', TRUE, 1), (97811, '2', FALSE, 2), (97811, '3', FALSE, 3), (97811, '4', FALSE, 4)");
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자는_문항을_등록할_수_없다() throws Exception {
        mockMvc.perform(post("/admin/activities/9781/questions").with(csrf())).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수업_편집_화면_활동_탭에_퀴즈별_문항과_유형이_보인다() throws Exception {
        mockMvc.perform(get("/admin/lessons/9781"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("저장된 문항")))
                .andExpect(content().string(Matchers.containsString("결과 유추하기")))
                .andExpect(content().string(Matchers.containsString("사용 중 1개")))
                .andExpect(content().string(Matchers.containsString("/admin/activities/9781/questions")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 문항을_추가하면_수업_편집으로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/activities/9781/questions").with(csrf())
                        .param("displayType", "CONCEPT").param("content", "새 문항").param("explanation", "해설")
                        .param("conceptTagId", "9781").param("choices", "가", "나", "다", "라").param("correctIndex", "0"))
                .andExpect(redirectedUrl("/admin/lessons/9781"))
                .andExpect(flash().attributeCount(0));
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quiz_question WHERE activity_id = 9781", Integer.class)).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 보기가_모자라고_코드가_없으면_칸_메시지와_함께_모달을_다시_연다() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/activities/9781/questions").with(csrf())
                        .param("displayType", "CODE_BLANK").param("content", "새 문항").param("explanation", "해설")
                        .param("conceptTagId", "9781").param("choices", "가", "", "다", "라").param("correctIndex", "0"))
                .andExpect(redirectedUrl("/admin/lessons/9781"))
                .andExpect(flash().attribute("quizQuestionTarget", "new-9781"))
                .andReturn();
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) result.getFlashMap().get("quizQuestionErrors");
        assertThat(errors).containsEntry("choices", "보기 4개를 모두 입력해 주세요.")
                .containsEntry("codeSnippet", "코드 빈칸 채우기·결과 유추하기 문항은 코드를 입력해 주세요.");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quiz_question WHERE activity_id = 9781", Integer.class)).isEqualTo(1);

        // A-06 이 flash 로 그 모달을 열고 칸 메시지를 보여준다
        mockMvc.perform(get("/admin/lessons/9781").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("코드 빈칸 채우기·결과 유추하기 문항은 코드를 입력해 주세요.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 사용_중지하면_수업_편집으로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/quiz-questions/97811/active").with(csrf()).param("active", "false"))
                .andExpect(redirectedUrl("/admin/lessons/9781"));
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT is_active FROM quiz_question WHERE quiz_question_id = 97811", Boolean.class)).isFalse();
    }
}
