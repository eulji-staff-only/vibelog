package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dao.CourseRepository;
import com.vibelog.domain.course.model.dto.CourseAdminDetail;
import com.vibelog.domain.course.model.dto.CourseAdminRow;
import com.vibelog.domain.course.model.dto.CourseForm;
import com.vibelog.domain.course.model.entity.Course;
import com.vibelog.domain.lesson.model.dto.CourseLessonRow;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 코스 관리 (API-ADM-11~15, FR-CRS-005, #96)
 * - 새 코스는 비공개로 저장되고 학습자 코스 목록(공개 코스만)에 보이지 않는다
 * - 수정은 공개 상태를 바꾸지 않는다
 * - 없는 트랙·카테고리·코스는 TRACK_NOT_FOUND·CATEGORY_NOT_FOUND·COURSE_NOT_FOUND
 * DB 에 샘플 데이터가 있을 수 있어 이 테스트가 넣은 값(997x, 이름 앞 "코관")만 본다.
 */
@SpringBootTest
@Transactional
class CourseAdminServiceTest {

    @Autowired
    CourseAdminService service;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES "
                + "(9971, '코관 트랙A', '대상', '설명', 9971), (9972, '코관 트랙B', '대상', '설명', 9972)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9971, '코관 카테고리', 9971)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9971, 9971, 9971, '코관 공개 코스', '한 줄', '소개', '대상', '목표', 7, TRUE), "
                + "(9972, 9972, 9971, '코관 비공개 코스', '한 줄', '소개', '대상', '목표', 7, FALSE)");
    }

    @Test
    void 새_코스는_비공개로_저장되고_학습자_코스_목록에_보이지_않는다() {
        Long courseId = service.createCourse(form(9971L, 9971L, "코관 새 코스"));
        entityManager.flush();
        entityManager.clear();

        Course saved = courseRepository.findById(courseId).orElseThrow();
        assertThat(saved.isPublic()).isFalse();
        assertThat(saved.getTitle()).isEqualTo("코관 새 코스");
        assertThat(saved.getPlanDays()).isEqualTo(14);
        assertThat(courseRepository.findByIsPublicTrueOrderByCourseIdAsc())
                .extracting(Course::getCourseId).doesNotContain(courseId);
    }

    @Test
    void 수정은_공개_상태를_그대로_둔다() {
        service.updateCourse(9971L, form(9972L, 9971L, "코관 고친 코스"));
        entityManager.flush();
        entityManager.clear();

        Course course = courseRepository.findById(9971L).orElseThrow();
        assertThat(course.isPublic()).isTrue();
        assertThat(course.getTitle()).isEqualTo("코관 고친 코스");
        assertThat(course.getTrackId()).isEqualTo(9972L);
    }

    @Test
    void 없는_트랙_카테고리_코스는_막는다() {
        assertError(() -> service.createCourse(form(9979L, 9971L, "코관 코스")), CourseErrorCode.TRACK_NOT_FOUND);
        assertError(() -> service.createCourse(form(9971L, 9979L, "코관 코스")), CourseErrorCode.CATEGORY_NOT_FOUND);
        assertError(() -> service.updateCourse(9979L, form(9971L, 9971L, "코관 코스")), CourseErrorCode.COURSE_NOT_FOUND);
        assertError(() -> service.getCourse(9979L), CourseErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void 목록은_트랙으로_거르고_트랙_카테고리_이름과_공개_상태를_싣는다() {
        List<CourseAdminRow> all = mine(service.getCourses(null, null));
        assertThat(all).extracting(CourseAdminRow::title).containsExactly("코관 공개 코스", "코관 비공개 코스");
        assertThat(all.get(0).trackName()).isEqualTo("코관 트랙A");
        assertThat(all.get(0).categoryName()).isEqualTo("코관 카테고리");
        assertThat(all).extracting(CourseAdminRow::isPublic).containsExactly(true, false);
        assertThat(all).extracting(CourseAdminRow::enrollmentCount).containsOnly(0L);

        assertThat(mine(service.getCourses(9972L, null))).extracting(CourseAdminRow::title).containsExactly("코관 비공개 코스");
        assertThat(mine(service.getCourses(null, 9971L))).hasSize(2);
    }

    @Test
    void 상세는_비공개_수업까지_순서대로_보여주고_과제가_있는지_알려준다() {
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9972, 9972, '코관 둘째 수업', '소개', '목표', '본문', 10, 2, FALSE), "
                + "(9971, 9972, '코관 첫 수업', '소개', '목표', '본문', 10, 1, TRUE)");

        CourseAdminDetail detail = service.getCourse(9972L);
        assertThat(detail.lessons()).extracting(CourseLessonRow::title).containsExactly("코관 첫 수업", "코관 둘째 수업");
        assertThat(detail.lessons()).extracting(CourseLessonRow::isPublic).containsExactly(true, false);
        assertThat(detail.trackName()).isEqualTo("코관 트랙B");
        assertThat(detail.hasAssignment()).isFalse();

        jdbcTemplate.update("INSERT INTO portfolio_assignment (course_id, topic, goal, agent_guide) VALUES (9972, '주제', '목표', '안내')");
        assertThat(service.getCourse(9972L).hasAssignment()).isTrue();
    }

    @Test
    void 공개된_수업과_포트폴리오_과제가_모두_있어야_공개된다() {
        // 9972: 비공개 코스. 수업은 비공개 하나뿐, 과제 없음
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) "
                + "VALUES (9973, 9972, '코관 비공개 수업', '소개', '목표', '본문', 10, 1, FALSE)");
        assertThat(service.changeVisibility(9972L, true)).isFalse();
        assertThat(service.getCourse(9972L).publishable()).isFalse();

        jdbcTemplate.update("INSERT INTO portfolio_assignment (course_id, topic, goal, agent_guide) VALUES (9972, '주제', '목표', '안내')");
        assertThat(service.changeVisibility(9972L, true)).isFalse();   // 과제만 있고 공개 수업 없음

        jdbcTemplate.update("UPDATE lesson SET is_public = TRUE WHERE lesson_id = 9973");
        assertThat(service.getCourse(9972L).publishable()).isTrue();
        assertThat(service.changeVisibility(9972L, true)).isTrue();
        entityManager.flush();
        entityManager.clear();
        assertThat(courseRepository.findByIsPublicTrueOrderByCourseIdAsc()).extracting(Course::getCourseId).contains(9972L);
    }

    @Test
    void 공개_수업이_있어도_과제가_없으면_공개되지_않는다() {
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) "
                + "VALUES (9974, 9972, '코관 공개 수업', '소개', '목표', '본문', 10, 1, TRUE)");
        assertThat(service.changeVisibility(9972L, true)).isFalse();
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT is_public FROM course WHERE course_id = 9972", Boolean.class)).isFalse();
    }

    @Test
    void 비공개는_조건_없이_되고_학습자_코스_목록에서_사라진다() {
        assertThat(service.changeVisibility(9971L, false)).isTrue();   // 9971: 공개 코스, 수업·과제 없음
        entityManager.flush();
        entityManager.clear();
        assertThat(courseRepository.findByIsPublicTrueOrderByCourseIdAsc()).extracting(Course::getCourseId).doesNotContain(9971L);
        assertError(() -> service.changeVisibility(9979L, false), CourseErrorCode.COURSE_NOT_FOUND);
    }

    private List<CourseAdminRow> mine(List<CourseAdminRow> rows) {
        return rows.stream().filter(row -> row.title().startsWith("코관")).toList();
    }

    private CourseForm form(Long trackId, Long categoryId, String title) {
        return CourseForm.of(trackId, categoryId, title, "한 줄 소개", "소개", "대상", "목표", 14);
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(expected);
    }
}
