package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dto.CompleteActivityResult;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
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
 * API-LSN-02 개념 읽기 완료를 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 회원은 02-seed 테스트 계정(1 학습자)을 쓰고, 코스·수업·활동은 테스트 안에서만 넣는다. 끝나면 모두 롤백된다.
 *
 * 테스트 코스 구조 (코스 9201)
 *  - 수업 1(9201, 공개): 개념(9201) → 퀴즈(9202, 마지막 활동)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9201, '테스트 트랙', '대상', '설명', 9201)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9201, '테스트 카테고리', 9201)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9201, 9201, 9201, '코스 9201', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9201, 9201, '수업 1', '소개', '목표', '본문', 30, 1, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9201, 9201, 'CONCEPT', '개념 1', '본문', 'MARKDOWN', 10, NULL, 1), "
                + "(9202, 9201, 'QUIZ', '퀴즈 1', NULL, NULL, 10, 50, 2)"
})
class LessonActivityServiceTest {

    static final long LEARNER = 1L;
    static final long COURSE = 9201L;
    static final long LESSON = 9201L;
    static final long CONCEPT = 9201L;
    static final long QUIZ = 9202L;

    @Autowired LessonActivityService lessonActivityService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;

    @Test
    void 활동이_잠겨_있으면_locked_결과를_돌려주고_완료를_기록하지_않는다() {
        Enrollment enrollment = enroll(); // 개념을 열지 않음

        CompleteActivityResult result = lessonActivityService.completeConcept(LEARNER, COURSE, LESSON, CONCEPT);

        assertThat(result.locked()).isTrue();
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), CONCEPT))
                .isEmpty();
    }

    @Test
    void 개념을_완료하면_바로_뒤_퀴즈가_열리고_퀴즈_화면으로_안내한다() {
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, CONCEPT);

        CompleteActivityResult result = lessonActivityService.completeConcept(LEARNER, COURSE, LESSON, CONCEPT);

        assertThat(result.locked()).isFalse();
        assertThat(result.redirectUrl()).isEqualTo("/courses/" + COURSE + "/lessons/" + LESSON + "/quizzes/" + QUIZ);
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), CONCEPT)
                .orElseThrow().isCompleted()).isTrue();
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), QUIZ)
                .orElseThrow().isUnlocked()).isTrue();
    }

    @Test
    void 완료_기록은_처음_한_번만_하고_다시_눌러도_같은_결과를_돌려준다() {
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, CONCEPT);

        lessonActivityService.completeConcept(LEARNER, COURSE, LESSON, CONCEPT);
        LocalDateTime firstCompletedAt = activityProgressRepository
                .findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), CONCEPT).orElseThrow().getCompletedAt();

        CompleteActivityResult second = lessonActivityService.completeConcept(LEARNER, COURSE, LESSON, CONCEPT);

        assertThat(second.locked()).isFalse();
        assertThat(second.redirectUrl()).isEqualTo("/courses/" + COURSE + "/lessons/" + LESSON + "/quizzes/" + QUIZ);
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), CONCEPT)
                .orElseThrow().getCompletedAt()).isEqualTo(firstCompletedAt);
    }

    @Test
    void 존재하지_않는_활동이면_ACTIVITY_NOT_FOUND() {
        enroll();

        assertThatThrownBy(() -> lessonActivityService.completeConcept(LEARNER, COURSE, LESSON, 99999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 퀴즈_활동은_개념_완료_요청으로_완료되지_않는다() {
        // 퀴즈는 정답률을 통과해야 완료된다 (퀴즈 영역 몫). 이 엔드포인트로 완료 처리하면 안 된다
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, QUIZ);

        assertThatThrownBy(() -> lessonActivityService.completeConcept(LEARNER, COURSE, LESSON, QUIZ))
                .isInstanceOf(BusinessException.class);
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), QUIZ)
                .orElseThrow().isCompleted()).isFalse();
    }

    private Enrollment enroll() {
        return enrollmentRepository.saveAndFlush(Enrollment.create(LEARNER, COURSE, LocalDateTime.now()));
    }

    private void unlockActivity(Enrollment enrollment, long activityId) {
        activityProgressRepository.saveAndFlush(
                ActivityProgress.create(enrollment.getEnrollmentId(), activityId, LocalDateTime.now()));
    }
}
