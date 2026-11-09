package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dto.CourseCard;
import com.vibelog.domain.course.model.dto.CourseDetailView;
import com.vibelog.domain.course.model.dto.CourseListView;
import com.vibelog.domain.lesson.model.policy.LessonSummary;
import com.vibelog.domain.course.model.dto.TrackCourseGroup;
import com.vibelog.domain.course.model.dto.TrackIntroView;
import com.vibelog.global.exception.BusinessException;
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
 * 트랙 소개(API-CRS-01)·코스 목록(API-CRS-02, #97)·코스 상세(API-CRS-03, #98)
 * - 비공개 코스는 어디에도 보이지 않는다. 상세로 직접 열어도 COURSE_NOT_FOUND (FR-CRS-001, 002, 004)
 * - 진단을 마친 회원(COMPLETED)에게만 추천 트랙이 보인다. 건너뜀·비회원은 없다
 * - 수업 수는 공개 수업만 센다 (계약 ⑪)
 * DB 에 다른 코스가 있을 수 있어 이 테스트가 넣은 ID(97xx)만 본다.
 */
@SpringBootTest
@Transactional
class CourseBrowseServiceTest {

    @Autowired
    CourseBrowseService courseBrowseService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES "
                + "(9701, '테스트 Starter', '입문 대상', '입문 설명', 9701), (9702, '테스트 Controller', '통제 대상', '통제 설명', 9702)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9701, '테스트 웹 기초', 9701)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) VALUES "
                + "(9701, 9701, 9701, '공개 입문 코스', '한 줄', '소개', '대상', '목표', 7, TRUE), "
                + "(9702, 9701, 9701, '비공개 입문 코스', '한 줄', '소개', '대상', '목표', 7, FALSE), "
                + "(9703, 9702, 9701, '공개 통제 코스', '한 줄', '소개', '대상', '목표', 14, TRUE)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9701, 9701, '공개 수업1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9702, 9701, '공개 수업2', '소개', '목표', '본문', 30, 2, TRUE), "
                + "(9703, 9701, '비공개 수업', '소개', '목표', '본문', 30, 3, FALSE)");
    }

    @Test
    void 트랙_소개는_공개_코스만_보이고_수업_수는_공개_수업만_센다() {
        TrackIntroView view = courseBrowseService.getTrackIntro(9701L);

        assertThat(view.name()).isEqualTo("테스트 Starter");
        assertThat(view.stage()).isEqualTo(9701);
        assertThat(view.courses()).extracting(CourseCard::title).containsExactly("공개 입문 코스");
        assertThat(view.courses().get(0).lessonCount()).isEqualTo(2);
        assertThat(view.courses().get(0).categoryName()).isEqualTo("테스트 웹 기초");
        assertThat(view.firstCourse().courseId()).isEqualTo(9701L);
    }

    @Test
    void 없는_트랙은_TRACK_NOT_FOUND() {
        assertThatThrownBy(() -> courseBrowseService.getTrackIntro(9999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(CourseErrorCode.TRACK_NOT_FOUND);
    }

    @Test
    void 코스_목록은_트랙별로_묶고_비공개_코스는_빠진다() {
        CourseListView view = courseBrowseService.getCourseList(null, null);

        assertThat(titlesOf(view, 9701L)).containsExactly("공개 입문 코스");
        assertThat(titlesOf(view, 9702L)).containsExactly("공개 통제 코스");
        assertThat(view.groups().stream().flatMap(g -> g.courses().stream()))
                .extracting(CourseCard::courseId).doesNotContain(9702L);
        assertThat(view.recommended()).isNull();   // 비회원
    }

    @Test
    void 트랙으로_거르면_그_트랙만_보이고_없는_트랙이면_빈_목록() {
        CourseListView filtered = courseBrowseService.getCourseList(null, 9702L);
        assertThat(filtered.groups()).extracting(TrackCourseGroup::trackId).containsExactly(9702L);
        assertThat(filtered.totalCount()).isEqualTo(1);

        CourseListView none = courseBrowseService.getCourseList(null, 9999L);
        assertThat(none.groups()).isEmpty();
        assertThat(none.isEmpty()).isTrue();
    }

    @Test
    void 진단을_마친_회원에게만_추천_트랙이_보인다() {
        long completed = member("browse-done@vibelog.test");
        long skipped = member("browse-skip@vibelog.test");
        long none = member("browse-none@vibelog.test");
        jdbcTemplate.update("INSERT INTO diagnosis_result (member_id, recommended_track_id, status) VALUES (?, 9702, 'COMPLETED')", completed);
        jdbcTemplate.update("INSERT INTO diagnosis_result (member_id, recommended_track_id, status) VALUES (?, NULL, 'SKIPPED')", skipped);

        TrackCourseGroup recommended = courseBrowseService.getCourseList(completed, null).recommended();
        assertThat(recommended.trackId()).isEqualTo(9702L);
        assertThat(recommended.courses()).extracting(CourseCard::title).containsExactly("공개 통제 코스");

        assertThat(courseBrowseService.getCourseList(skipped, null).recommended()).isNull();
        assertThat(courseBrowseService.getCourseList(none, null).recommended()).isNull();
    }

    @Test
    void 코스_상세는_공개_수업만_순서대로_보여준다() {
        CourseDetailView view = courseBrowseService.getCourseDetail(null, 9701L);

        assertThat(view.trackName()).isEqualTo("테스트 Starter");
        assertThat(view.categoryName()).isEqualTo("테스트 웹 기초");
        assertThat(view.lessons()).extracting(LessonSummary::title).containsExactly("공개 수업1", "공개 수업2");
        assertThat(view.lessonCount()).isEqualTo(2);
    }

    @Test
    void 없거나_비공개인_코스_상세는_COURSE_NOT_FOUND() {
        assertThatThrownBy(() -> courseBrowseService.getCourseDetail(null, 9702L))   // 비공개
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(CourseErrorCode.COURSE_NOT_FOUND);
        assertThatThrownBy(() -> courseBrowseService.getCourseDetail(null, 9999L))   // 없음
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(CourseErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void 코스_상세의_수강_상태는_비회원_NONE_수강중_ENROLLED_완료_COMPLETED() {
        long notEnrolled = member("detail-none@vibelog.test");
        long enrolled = member("detail-ing@vibelog.test");
        long completed = member("detail-done@vibelog.test");
        jdbcTemplate.update("INSERT INTO enrollment (member_id, course_id, started_at) VALUES (?, 9701, NOW())", enrolled);
        jdbcTemplate.update("INSERT INTO enrollment (member_id, course_id, started_at, completed_at) VALUES (?, 9701, NOW(), NOW())", completed);

        assertThat(courseBrowseService.getCourseDetail(null, 9701L).enrollmentStatus()).isEqualTo("NONE");
        assertThat(courseBrowseService.getCourseDetail(notEnrolled, 9701L).enrollmentStatus()).isEqualTo("NONE");
        assertThat(courseBrowseService.getCourseDetail(enrolled, 9701L).enrollmentStatus()).isEqualTo("ENROLLED");
        assertThat(courseBrowseService.getCourseDetail(completed, 9701L).enrollmentStatus()).isEqualTo("COMPLETED");
        assertThat(courseBrowseService.getCourseDetail(completed, 9701L).isEnrolled()).isTrue();
    }

    private List<String> titlesOf(CourseListView view, Long trackId) {
        return view.groups().stream()
                .filter(group -> group.trackId().equals(trackId))
                .flatMap(group -> group.courses().stream())
                .map(CourseCard::title)
                .toList();
    }

    private long member(String email) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', ?)", email, email.substring(0, 11));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
