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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 운영자 수업 만들기·순서·편집(A-06, API-ADM-17~20, FR-LSN-011, #100). 넣은 데이터는 테스트가 끝나면 되돌린다(@Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LessonAdminTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired LessonRepository lessonRepository;

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자는_들어올_수_없다() throws Exception {
        long courseId = courseOf();
        long lessonId = lessonOf(courseId, "수업 1", 1, true);

        mockMvc.perform(get("/admin/lessons/" + lessonId)).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수업을_만들면_비공개로_저장되고_편집_화면으로_이동한다() throws Exception {
        long courseId = courseOf();

        mockMvc.perform(post("/admin/courses/{courseId}/lessons", courseId)
                        .with(csrf())
                        .param("title", "새 수업")
                        .param("intro", "소개")
                        .param("goal", "목표")
                        .param("contents", "본문")
                        .param("estimatedMinutes", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/lessons/*"));

        Boolean isPublic = jdbcTemplate.queryForObject(
                "SELECT is_public FROM lesson WHERE course_id = ? AND title = '새 수업'", Boolean.class, courseId);
        assertIsFalse(isPublic);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수업_만들기_제목이_비어있으면_오류다() throws Exception {
        long courseId = courseOf();

        mockMvc.perform(post("/admin/courses/{courseId}/lessons", courseId)
                        .with(csrf())
                        .param("title", "")
                        .param("intro", "소개")
                        .param("goal", "목표")
                        .param("contents", "본문")
                        .param("estimatedMinutes", "15"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("수업 제목을 입력해 주세요")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수업_순서를_저장하면_그_순서가_유지된다() throws Exception {
        long courseId = courseOf();
        long lesson1 = lessonOf(courseId, "수업 1", 1, true);
        long lesson2 = lessonOf(courseId, "수업 2", 2, false);

        mockMvc.perform(post("/admin/courses/{courseId}/lessons/order", courseId)
                        .with(csrf())
                        .param("lessonIds", String.valueOf(lesson2), String.valueOf(lesson1)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses/" + courseId));

        // 바뀐 sort_order는 더티 체킹으로 지연 반영되어 raw JDBC로는 아직 안 보일 수 있어 JPA 조회(Repository)로 확인한다
        org.assertj.core.api.Assertions.assertThat(lessonRepository.findById(lesson2).orElseThrow().getSortOrder()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(lessonRepository.findById(lesson1).orElseThrow().getSortOrder()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수업_순서_목록이_안_맞으면_오류다() throws Exception {
        long courseId = courseOf();
        long lesson1 = lessonOf(courseId, "수업 1", 1, true);
        lessonOf(courseId, "수업 2", 2, false);

        mockMvc.perform(post("/admin/courses/{courseId}/lessons/order", courseId)
                        .with(csrf())
                        .param("lessonIds", String.valueOf(lesson1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 편집_화면에_수업_정보와_활동_모의고사_문항이_보인다() throws Exception {
        long courseId = courseOf();
        long lessonId = lessonOf(courseId, "수업 1", 1, true);
        long conceptActivityId = activityOf(lessonId, "CONCEPT", "개념 1-1", 1, true);
        activityOf(lessonId, "QUIZ", "퀴즈 1-1", 2, false);
        conceptTagOf("변수");
        examQuestionOf(lessonId, conceptActivityId, "문항 내용");

        mockMvc.perform(get("/admin/lessons/" + lessonId))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("코스 " + courseId),
                        containsString("개념 1-1"),
                        containsString("퀴즈 1-1"),
                        containsString("문항 내용"),
                        containsString("변수"),
                        containsString("아직 등록된 문항이 없습니다"))));   // 퀴즈 문항 조각 (quiz #102)
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수업_정보를_수정하면_바뀐_값으로_다시_보인다() throws Exception {
        long courseId = courseOf();
        long lessonId = lessonOf(courseId, "수업 1", 1, true);

        mockMvc.perform(post("/admin/lessons/{lessonId}/edit", lessonId)
                        .with(csrf())
                        .param("title", "바뀐 제목")
                        .param("intro", "소개")
                        .param("goal", "목표")
                        .param("contents", "본문")
                        .param("estimatedMinutes", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/lessons/" + lessonId));

        mockMvc.perform(get("/admin/lessons/" + lessonId))
                .andExpect(content().string(containsString("바뀐 제목")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수업_정보_수정이_틀리면_같은_화면에_오류_메시지가_보인다() throws Exception {
        long courseId = courseOf();
        long lessonId = lessonOf(courseId, "수업 1", 1, true);

        mockMvc.perform(post("/admin/lessons/{lessonId}/edit", lessonId)
                        .with(csrf())
                        .param("title", "")
                        .param("intro", "소개")
                        .param("goal", "목표")
                        .param("contents", "본문")
                        .param("estimatedMinutes", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("수업 제목을 입력해 주세요")));
    }

    private void assertIsFalse(Boolean value) {
        org.assertj.core.api.Assertions.assertThat(value).isFalse();
    }

    private long courseOf() {
        jdbcTemplate.update("INSERT INTO track (name, target, description, sort_order) VALUES ('트랙', '대상', '설명', 9901)");
        Long trackId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO course_category (name, sort_order) VALUES ('카테고리', 1)");
        Long categoryId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO course (track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                        + "VALUES (?, ?, '임시 제목', '요약', '설명', '대상', '목표', 7, TRUE)",
                trackId, categoryId);
        Long courseId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("UPDATE course SET title = ? WHERE course_id = ?", "코스 " + courseId, courseId);
        return courseId;
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

    private void conceptTagOf(String name) {
        jdbcTemplate.update("INSERT INTO concept_tag (name) VALUES (?)", name);
    }

    private void examQuestionOf(long lessonId, long conceptActivityId, String content) {
        jdbcTemplate.update("INSERT INTO exam_question (lesson_id, concept_activity_id, content, explanation, sort_order) "
                + "VALUES (?, ?, ?, '해설', 1)", lessonId, conceptActivityId, content);
    }
}
