package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * API-ENR-01 수강 시작을 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 회원은 02-seed 테스트 계정(1 학습자)을 쓰고, 코스·수업·활동은 테스트 안에서만 넣는다. 끝나면 모두 롤백된다.
 *
 * 테스트 코스 구조 (코스 8001)
 *  - 수업 1(8001, 공개): 개념(8001) → 퀴즈(8002)
 *  - 수업 2(8002, 공개): 개념(8003)
 *  - 수업 3(8003, 비공개): 퀴즈(8004)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (8001, '테스트 트랙', '대상', '설명', 8001)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (8001, '테스트 카테고리', 8001)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (8001, 8001, 8001, '코스 8001', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(8001, 8001, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(8002, 8001, '수업 2', '소개', '목표', '본문', 20, 2, TRUE), "
                + "(8003, 8001, '수업 3(비공개)', '소개', '목표', '본문', 10, 3, FALSE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(8001, 8001, 'CONCEPT', '개념 1-1', '본문', 'MARKDOWN', 10, NULL, 1), "
                + "(8002, 8001, 'QUIZ', '퀴즈 1-1', NULL, NULL, 10, 50, 2), "
                + "(8003, 8002, 'CONCEPT', '개념 2-1', '본문', 'MARKDOWN', 10, NULL, 1), "
                + "(8004, 8003, 'QUIZ', '퀴즈 3-1', NULL, NULL, 10, 50, 1)"
})
class EnrollmentServiceTest {

    static final long LEARNER = 1L;
    static final long COURSE = 8001L;
    static final long LESSON_1 = 8001L;
    static final long LESSON_2 = 8002L;
    static final long FIRST_ACTIVITY = 8001L;

    @Autowired EnrollmentService enrollmentService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;

    @Test
    void 수강을_시작하면_공개_수업마다_진도가_생기고_첫_수업_첫_개념만_열린다() {
        enrollmentService.enroll(LEARNER, COURSE);

        Enrollment enrollment = enrollmentRepository.findByMemberIdAndCourseId(LEARNER, COURSE).orElseThrow();
        assertThat(enrollment.getStartedAt()).isNotNull();

        LessonProgress lesson1Progress = lessonProgress(enrollment, LESSON_1);
        LessonProgress lesson2Progress = lessonProgress(enrollment, LESSON_2);
        assertThat(lesson1Progress.isUnlocked()).isTrue();
        assertThat(lesson2Progress.isUnlocked()).isFalse();

        ActivityProgress firstActivityProgress = activityProgressRepository
                .findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), FIRST_ACTIVITY)
                .orElseThrow();
        assertThat(firstActivityProgress.isUnlocked()).isTrue();
    }

    @Test
    void 같은_코스를_두_번_시작해도_수강_기록은_하나다() {
        enrollmentService.enroll(LEARNER, COURSE);
        Long firstEnrollmentId = enrollmentRepository.findByMemberIdAndCourseId(LEARNER, COURSE)
                .orElseThrow().getEnrollmentId();

        enrollmentService.enroll(LEARNER, COURSE);

        Long secondEnrollmentId = enrollmentRepository.findByMemberIdAndCourseId(LEARNER, COURSE)
                .orElseThrow().getEnrollmentId();
        assertThat(secondEnrollmentId).isEqualTo(firstEnrollmentId);
    }

    private LessonProgress lessonProgress(Enrollment enrollment, long lessonId) {
        return lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollment.getEnrollmentId(), lessonId)
                .orElseThrow();
    }
}
