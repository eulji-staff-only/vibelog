package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dto.ActivityStatus;
import com.vibelog.domain.lesson.model.dto.LessonActivityCard;
import com.vibelog.domain.lesson.model.dto.LessonPageData;
import com.vibelog.domain.lesson.model.dto.LessonPageResult;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 운영자 수업 미리보기(API-LSN-01 운영자 예외, API-ADM-39, FR-LSN-015)를 실제 DB로 확인한다.
 * 코스·수업·활동은 테스트 안에서만 넣고(id 9650대) 끝나면 롤백된다. 운영자는 02-seed 의 2번, 학습자는 1번.
 *
 * 테스트 코스(9650): 비공개 수업(9650) = 개념(9650, sort 1) → 퀴즈(9651, sort 2) → 개념(9652, sort 3)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9650, '미리보기 트랙', '대상', '설명', 9650)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9650, '미리보기 카테고리', 9650)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9650, 9650, 9650, '미리보기 코스', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9650, 9650, '비공개 수업', '소개', '목표', '본문', 30, 1, FALSE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9650, 9650, 'CONCEPT', '개념 A', '본문 A', 'MARKDOWN', 10, NULL, 1), "
                + "(9651, 9650, 'QUIZ', '퀴즈 A', NULL, NULL, 10, 50, 2), "
                + "(9652, 9650, 'CONCEPT', '개념 B', '본문 B', 'MARKDOWN', 10, NULL, 3)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order, is_active) VALUES "
                + "(9653, 9650, 'CONCEPT', '사용 중지 개념', '본문', 'MARKDOWN', 10, NULL, 0, FALSE)"
})
class LessonPagePreviewServiceTest {

    static final long ADMIN = 2L;
    static final long LEARNER = 1L;
    static final long COURSE = 9650L;
    static final long LESSON = 9650L;

    @Autowired LessonPageService lessonPageService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;

    @Test
    void 운영자는_수강하지_않아도_비공개_잠긴_수업을_본다() {
        LessonPageResult result = lessonPageService.getPage(ADMIN, COURSE, LESSON, null, true);

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.OK);
        LessonPageData data = result.data();
        assertThat(data.preview()).isTrue();
        assertThat(data.cards()).hasSize(4); // 사용 중인 활동 3개 + 모의고사. 사용 중지 활동은 빠진다
        assertThat(data.cards()).noneMatch(LessonActivityCard::isLocked);
        assertThat(data.cards()).extracting(LessonActivityCard::status).containsOnly(ActivityStatus.IN_PROGRESS);
    }

    @Test
    void 운영자_기본_선택은_첫_번째_사용_중인_활동이다() {
        LessonPageData data = lessonPageService.getPage(ADMIN, COURSE, LESSON, null, true).data();

        assertThat(data.cards().get(0).activityId()).isEqualTo(9650L);
        assertThat(data.cards().get(0).selected()).isTrue();
        assertThat(data.cards()).filteredOn(LessonActivityCard::selected).hasSize(1);
        assertThat(data.cards().get(0).body()).contains("본문 A");
    }

    @Test
    void 운영자도_activity_쿼리로_다른_활동을_고를_수_있다() {
        LessonPageData data = lessonPageService.getPage(ADMIN, COURSE, LESSON, "9652", true).data();

        assertThat(data.cards().get(2).selected()).isTrue();
        assertThat(data.cards().get(0).selected()).isFalse();
    }

    @Test
    void 운영자_미리보기는_진행_기록을_만들지_않는다() {
        long enrollmentsBefore = enrollmentRepository.count();
        long lessonProgressBefore = lessonProgressRepository.count();
        long activityProgressBefore = activityProgressRepository.count();

        lessonPageService.getPage(ADMIN, COURSE, LESSON, null, true);
        lessonPageService.getPage(ADMIN, COURSE, LESSON, "exam", true);

        assertThat(enrollmentRepository.count()).isEqualTo(enrollmentsBefore);
        assertThat(lessonProgressRepository.count()).isEqualTo(lessonProgressBefore);
        assertThat(activityProgressRepository.count()).isEqualTo(activityProgressBefore);
    }

    @Test
    void 학습자는_같은_수업이라도_수강하지_않았으면_수강_안_함으로_본다() {
        LessonPageResult result = lessonPageService.getPage(LEARNER, COURSE, LESSON, null, false);

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.NOT_ENROLLED);
    }

    @Test
    void 학습자는_수강해도_수업이_안_열렸으면_잠김이다() {
        enrollmentRepository.saveAndFlush(Enrollment.create(LEARNER, COURSE, LocalDateTime.now()));

        LessonPageResult result = lessonPageService.getPage(LEARNER, COURSE, LESSON, null, false);

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.LOCKED);
        assertThat(result.data()).isNull();
    }

    @Test
    void 미리보기여도_없는_수업이나_다른_코스의_수업은_LESSON_NOT_FOUND() {
        assertThatThrownBy(() -> lessonPageService.getPage(ADMIN, COURSE, 9699L, null, true))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> lessonPageService.getPage(ADMIN, 9651L, LESSON, null, true))
                .isInstanceOf(BusinessException.class);
    }
}
