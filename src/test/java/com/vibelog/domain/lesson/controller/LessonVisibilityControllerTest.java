package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.lesson.model.dao.LessonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 운영자 수업 공개·비공개(A-06, API-ADM-32·33, FR-LSN-014, PL-LSN-003, #118). 넣은 데이터는 테스트가 끝나면 되돌린다(@Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LessonVisibilityControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired LessonRepository lessonRepository;

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자는_공개_요청을_보낼_수_없다() throws Exception {
        long courseId = courseOf();
        long lessonId = lessonOf(courseId, "수업 1", 1, false);

        mockMvc.perform(post("/admin/lessons/{lessonId}/visibility", lessonId)
                        .with(csrf())
                        .param("isPublic", "true"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 필수_항목을_다_채우면_공개되고_코스_화면으로_이동한다() throws Exception {
        long courseId = courseOf();
        long lessonId = lessonOf(courseId, "수업 1", 1, false);
        long conceptActivityId = activityOf(lessonId, "CONCEPT", "개념 1-1", 1, true);
        long quizActivityId = activityOf(lessonId, "QUIZ", "퀴즈 1-1", 2, true);
        long conceptTagId = conceptTagOf("태그");
        quizQuestionOf(quizActivityId, conceptTagId, 3);
        examQuestionOf(lessonId, conceptActivityId, true);

        mockMvc.perform(post("/admin/lessons/{lessonId}/visibility", lessonId)
                        .with(csrf())
                        .param("isPublic", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses/" + courseId));

        // 더티 체킹으로 지연 반영되어 raw JDBC로는 아직 안 보일 수 있어 JPA 조회(Repository)로 확인한다
        assertThat(lessonRepository.findById(lessonId).orElseThrow().isPublic()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 퀴즈_문항이_2개뿐이면_공개되지_않고_편집_화면으로_돌아간다() throws Exception {
        long courseId = courseOf();
        long lessonId = lessonOf(courseId, "수업 1", 1, false);
        long conceptActivityId = activityOf(lessonId, "CONCEPT", "개념 1-1", 1, true);
        long quizActivityId = activityOf(lessonId, "QUIZ", "퀴즈 1-1", 2, true);
        long conceptTagId = conceptTagOf("태그");
        quizQuestionOf(quizActivityId, conceptTagId, 2);
        examQuestionOf(lessonId, conceptActivityId, true);

        mockMvc.perform(post("/admin/lessons/{lessonId}/visibility", lessonId)
                        .with(csrf())
                        .param("isPublic", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/lessons/" + lessonId));

        assertThat(lessonRepository.findById(lessonId).orElseThrow().isPublic()).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 영향_확인_API가_완료_포기하지_않은_수강_수를_JSON으로_돌려준다() throws Exception {
        long courseId = courseOf();
        long lessonId = lessonOf(courseId, "수업 1", 1, true);
        long activeMemberId = memberOf("active-learner");
        long droppedMemberId = memberOf("dropped-learner");
        enrollmentOf(activeMemberId, courseId, false, false);
        enrollmentOf(droppedMemberId, courseId, false, true);

        mockMvc.perform(get("/api/admin/lessons/{lessonId}/impact", lessonId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.activeLearnerCount").value(1));
    }

    private long courseOf() {
        jdbcTemplate.update("INSERT INTO track (name, target, description, sort_order) VALUES ('트랙', '대상', '설명', 1)");
        Long trackId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO course_category (name, sort_order) VALUES ('카테고리', 1)");
        Long categoryId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO course (track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                        + "VALUES (?, ?, '코스', '요약', '설명', '대상', '목표', 7, TRUE)",
                trackId, categoryId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long lessonOf(long courseId, String title, int sortOrder, boolean isPublic) {
        jdbcTemplate.update("INSERT INTO lesson (course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) "
                        + "VALUES (?, ?, '소개', '목표', '본문', 30, ?, ?)",
                courseId, title, sortOrder, isPublic);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long activityOf(long lessonId, String activityType, String title, int sortOrder, boolean active) {
        jdbcTemplate.update("INSERT INTO activity (lesson_id, activity_type, title, estimated_minutes, sort_order, is_active) "
                        + "VALUES (?, ?, ?, 10, ?, ?)",
                lessonId, activityType, title, sortOrder, active);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long conceptTagOf(String name) {
        jdbcTemplate.update("INSERT INTO concept_tag (name) VALUES (?)", name);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void quizQuestionOf(long activityId, long conceptTagId, int count) {
        for (int i = 1; i <= count; i++) {
            jdbcTemplate.update("INSERT INTO quiz_question (activity_id, concept_tag_id, display_type, content, explanation, sort_order, is_active) "
                            + "VALUES (?, ?, 'CONCEPT', ?, '해설', ?, TRUE)",
                    activityId, conceptTagId, "문항" + i, i);
        }
    }

    private void examQuestionOf(long lessonId, long conceptActivityId, boolean active) {
        jdbcTemplate.update("INSERT INTO exam_question (lesson_id, concept_activity_id, content, explanation, sort_order, is_active) "
                + "VALUES (?, ?, '모의고사 문항', '해설', 1, ?)", lessonId, conceptActivityId, active);
    }

    private long memberOf(String emailPrefix) {
        jdbcTemplate.update("INSERT INTO member (email, nickname) VALUES (?, '학습자')", emailPrefix + "@vibelog.test");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void enrollmentOf(long memberId, long courseId, boolean completed, boolean dropped) {
        jdbcTemplate.update("INSERT INTO enrollment (member_id, course_id, started_at, completed_at, dropped_at) "
                        + "VALUES (?, ?, NOW(), ?, ?)",
                memberId, courseId, completed ? java.time.LocalDateTime.now() : null, dropped ? java.time.LocalDateTime.now() : null);
    }
}
