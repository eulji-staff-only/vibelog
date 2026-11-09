package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.domain.lesson.model.policy.CourseLessonInfo;
import com.vibelog.domain.lesson.model.policy.ExamLessonProgress;
import com.vibelog.domain.lesson.model.policy.LessonSummary;
import com.vibelog.domain.lesson.model.policy.LoginEnrollmentChecker;
import com.vibelog.domain.lesson.model.policy.NextStep;
import com.vibelog.domain.lesson.model.policy.PortfolioAccess;
import com.vibelog.domain.lesson.model.policy.PortfolioEnrollmentProgress;
import com.vibelog.domain.lesson.model.policy.QuizActivityInfo;
import com.vibelog.domain.lesson.model.policy.QuizActivityProgress;
import com.vibelog.domain.lesson.model.policy.ReportNextStep;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * lesson 계약 ①②③⑥⑦⑨⑪⑫을 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 회원은 02-seed 테스트 계정(1 학습자)을 쓰고, 코스·수업·활동은 테스트 안에서만 넣는다. 끝나면 모두 롤백된다.
 *
 * 테스트 코스 구조 (코스 A, 9001)
 *  - 수업 1(9001, 공개): 개념1(9001) → 퀴즈1(9002, QUIZ_MID) → 개념2(9003, CONCEPT_MID) → 퀴즈2(9004, QUIZ_LAST_L1)
 *  - 수업 2(9002, 공개, 마지막 공개 수업): 개념(9005) → 퀴즈(9006)
 *  - 수업 3(9003, 비공개): 퀴즈만(9007, QUIZ_L3)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9001, '테스트 트랙', '대상', '설명', 9001)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9001, '테스트 카테고리', 9001)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9001, 9001, 9001, '코스 A', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9001, 9001, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9002, 9001, '수업 2', '소개', '목표', '본문', 20, 2, TRUE), "
                + "(9003, 9001, '수업 3(비공개)', '소개', '목표', '본문', 10, 3, FALSE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9001, 9001, 'CONCEPT', '개념 1-1', '본문', 'MARKDOWN', 10, NULL, 1), "
                + "(9002, 9001, 'QUIZ', '퀴즈 1-1', NULL, NULL, 10, 50, 2), "
                + "(9003, 9001, 'CONCEPT', '개념 1-2', '본문', 'MARKDOWN', 10, NULL, 3), "
                + "(9004, 9001, 'QUIZ', '퀴즈 1-2', NULL, NULL, 10, 50, 4), "
                + "(9005, 9002, 'CONCEPT', '개념 2-1', '본문', 'MARKDOWN', 10, NULL, 1), "
                + "(9006, 9002, 'QUIZ', '퀴즈 2-1', NULL, NULL, 10, 50, 2), "
                + "(9007, 9003, 'QUIZ', '퀴즈 3-1', NULL, NULL, 10, 50, 1)"
})
class LessonContractServiceTest {

    static final long LEARNER = 1L;
    static final long COURSE_A = 9001L;
    static final long LESSON_1 = 9001L;
    static final long LESSON_2 = 9002L;
    static final long CONCEPT_1 = 9001L;
    static final long QUIZ_MID = 9002L;
    static final long CONCEPT_MID = 9003L;
    static final long QUIZ_LAST_L1 = 9004L;
    static final long QUIZ_L3 = 9007L;

    @Autowired QuizActivityProgress quizActivityProgress;
    @Autowired ExamLessonProgress examLessonProgress;
    @Autowired PortfolioEnrollmentProgress portfolioEnrollmentProgress;
    @Autowired CourseLessonInfo courseLessonInfo;
    @Autowired LoginEnrollmentChecker loginEnrollmentChecker;

    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired EntityManager em;

    @Test
    void 계약1_getQuizActivity_수강_안_했으면_잠김() {
        QuizActivityInfo info = quizActivityProgress.getQuizActivity(LEARNER, QUIZ_MID);

        assertThat(info.courseId()).isEqualTo(COURSE_A);
        assertThat(info.lessonId()).isEqualTo(LESSON_1);
        assertThat(info.unlocked()).isFalse();
        assertThat(info.passRate()).isEqualTo(50);
    }

    @Test
    void 계약1_getQuizActivity_열려_있으면_unlocked_true() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        unlockActivity(enrollment, QUIZ_MID);

        assertThat(quizActivityProgress.getQuizActivity(LEARNER, QUIZ_MID).unlocked()).isTrue();
    }

    @Test
    void 계약1_recordQuizPassed_처음_한_번만_완료_기록하고_다음_개념이_열린다() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        unlockActivity(enrollment, QUIZ_MID);

        NextStep step1 = quizActivityProgress.recordQuizPassed(LEARNER, QUIZ_MID);
        LocalDateTime firstCompletedAt = activityProgress(enrollment, QUIZ_MID).getCompletedAt();

        NextStep step2 = quizActivityProgress.recordQuizPassed(LEARNER, QUIZ_MID);

        assertThat(step1.type()).isEqualTo(NextStep.CONCEPT);
        assertThat(step1.courseId()).isEqualTo(COURSE_A);
        assertThat(step1.lessonId()).isEqualTo(LESSON_1);
        assertThat(step1.activityId()).isEqualTo(CONCEPT_MID);
        assertThat(activityProgress(enrollment, CONCEPT_MID).isUnlocked()).isTrue();
        // 다시 통과를 기록해도 완료 시각이 바뀌지 않는다 (처음 한 번만)
        assertThat(activityProgress(enrollment, QUIZ_MID).getCompletedAt()).isEqualTo(firstCompletedAt);
        assertThat(step2.type()).isEqualTo(NextStep.CONCEPT);
        assertThat(step2.activityId()).isEqualTo(CONCEPT_MID);
    }

    @Test
    void 계약1_recordQuizPassed_공개_수업의_마지막_퀴즈면_EXAM() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        unlockActivity(enrollment, QUIZ_LAST_L1);

        NextStep step = quizActivityProgress.recordQuizPassed(LEARNER, QUIZ_LAST_L1);

        assertThat(step.type()).isEqualTo(NextStep.EXAM);
        assertThat(step.lessonId()).isEqualTo(LESSON_1);
        assertThat(step.activityId()).isNull();
    }

    @Test
    void 계약1_recordQuizPassed_비공개_수업의_마지막_퀴즈면_NONE() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        unlockActivity(enrollment, QUIZ_L3);

        NextStep step = quizActivityProgress.recordQuizPassed(LEARNER, QUIZ_L3);

        assertThat(step.type()).isEqualTo(NextStep.NONE);
        assertThat(step.activityId()).isNull();
    }

    @Test
    void 계약2_isExamOpen_마지막_퀴즈를_완료해야_true() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        unlockActivity(enrollment, QUIZ_LAST_L1);

        assertThat(examLessonProgress.isExamOpen(LEARNER, LESSON_1)).isFalse();

        quizActivityProgress.recordQuizPassed(LEARNER, QUIZ_LAST_L1);

        assertThat(examLessonProgress.isExamOpen(LEARNER, LESSON_1)).isTrue();
    }

    @Test
    void 계약3_recordReportViewed_처음_한_번만_완료_기록하고_다음_수업이_열린다() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        unlockLesson(enrollment, LESSON_1);

        ReportNextStep step1 = examLessonProgress.recordReportViewed(LEARNER, LESSON_1);
        LocalDateTime firstCompletedAt = lessonProgress(enrollment, LESSON_1).getCompletedAt();

        ReportNextStep step2 = examLessonProgress.recordReportViewed(LEARNER, LESSON_1);

        assertThat(step1.type()).isEqualTo(ReportNextStep.NEXT_LESSON);
        assertThat(step1.courseId()).isEqualTo(COURSE_A);
        assertThat(step1.nextLessonId()).isEqualTo(LESSON_2);
        assertThat(lessonProgress(enrollment, LESSON_2).isUnlocked()).isTrue();
        assertThat(activityProgress(enrollment, 9005L).isUnlocked()).isTrue();
        // 다시 확인해도 완료 시각이 바뀌지 않는다 (처음 한 번만)
        assertThat(lessonProgress(enrollment, LESSON_1).getCompletedAt()).isEqualTo(firstCompletedAt);
        assertThat(step2.type()).isEqualTo(ReportNextStep.NEXT_LESSON);
        assertThat(step2.nextLessonId()).isEqualTo(LESSON_2);
    }

    @Test
    void 계약3_recordReportViewed_마지막_공개_수업이면_포트폴리오가_열린다() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        unlockLesson(enrollment, LESSON_2);

        ReportNextStep step = examLessonProgress.recordReportViewed(LEARNER, LESSON_2);

        assertThat(step.type()).isEqualTo(ReportNextStep.PORTFOLIO);
        assertThat(step.nextLessonId()).isNull();
        assertThat(enrollmentRepository.findById(enrollment.getEnrollmentId()).orElseThrow().isPortfolioUnlocked())
                .isTrue();
    }

    @Test
    void 계약6_getAccess_수강_여부와_포트폴리오_열림_여부() {
        assertThat(portfolioEnrollmentProgress.getAccess(LEARNER, COURSE_A)).isEqualTo(new PortfolioAccess(false, false));

        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        assertThat(portfolioEnrollmentProgress.getAccess(LEARNER, COURSE_A)).isEqualTo(new PortfolioAccess(true, false));

        enrollment.unlockPortfolio();
        em.flush();
        assertThat(portfolioEnrollmentProgress.getAccess(LEARNER, COURSE_A)).isEqualTo(new PortfolioAccess(true, true));
    }

    @Test
    void 계약7_recordSubmitted_공개_수업을_모두_완료해야_처음_한_번만_코스를_완료한다() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        completeLesson(enrollment, LESSON_1);

        portfolioEnrollmentProgress.recordSubmitted(LEARNER, COURSE_A);
        assertThat(enrollmentRepository.findById(enrollment.getEnrollmentId()).orElseThrow().isCompleted()).isFalse();

        completeLesson(enrollment, LESSON_2);
        portfolioEnrollmentProgress.recordSubmitted(LEARNER, COURSE_A);
        LocalDateTime firstCompletedAt = enrollmentRepository.findById(enrollment.getEnrollmentId())
                .orElseThrow().getCompletedAt();

        portfolioEnrollmentProgress.recordSubmitted(LEARNER, COURSE_A);

        assertThat(enrollmentRepository.findById(enrollment.getEnrollmentId()).orElseThrow().getCompletedAt())
                .isEqualTo(firstCompletedAt);
    }

    @Test
    void 계약7_recordDeleted_완료_기록을_비운다() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        enrollment.complete();
        em.flush();

        portfolioEnrollmentProgress.recordDeleted(LEARNER, COURSE_A);

        assertThat(enrollmentRepository.findById(enrollment.getEnrollmentId()).orElseThrow().isCompleted()).isFalse();
    }

    @Test
    void 계약9_공개_수업이_있으면_true() {
        assertThat(courseLessonInfo.hasPublicLesson(COURSE_A)).isTrue();
        assertThat(courseLessonInfo.hasPublicLesson(99999L)).isFalse();
    }

    @Test
    void 계약11_getEnrollmentStatus_NONE_ENROLLED_COMPLETED() {
        assertThat(courseLessonInfo.getEnrollmentStatus(LEARNER, COURSE_A)).isEqualTo("NONE");

        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        assertThat(courseLessonInfo.getEnrollmentStatus(LEARNER, COURSE_A)).isEqualTo("ENROLLED");

        enrollment.complete();
        em.flush();
        assertThat(courseLessonInfo.getEnrollmentStatus(LEARNER, COURSE_A)).isEqualTo("COMPLETED");
    }

    @Test
    void 계약11_getEnrollmentStatus_포기가_완료보다_우선해_NONE() {
        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        enrollment.drop();
        enrollment.complete(); // 포기한 동안 포트폴리오를 제출해 완료 시각이 기록된 경우
        em.flush();
        assertThat(courseLessonInfo.getEnrollmentStatus(LEARNER, COURSE_A)).isEqualTo("NONE");

        enrollment.restart(); // 다시 시작하면 그 완료 기록이 반영된다
        em.flush();
        assertThat(courseLessonInfo.getEnrollmentStatus(LEARNER, COURSE_A)).isEqualTo("COMPLETED");
    }

    @Test
    void 계약11_getPublicLessons_공개_수업만_sort_order_순으로() {
        List<LessonSummary> lessons = courseLessonInfo.getPublicLessons(COURSE_A);

        assertThat(lessons).hasSize(2);
        assertThat(lessons.get(0).lessonId()).isEqualTo(LESSON_1);
        assertThat(lessons.get(0).conceptCount()).isEqualTo(2);
        assertThat(lessons.get(0).quizCount()).isEqualTo(2);
        assertThat(lessons.get(1).lessonId()).isEqualTo(LESSON_2);
        assertThat(lessons.get(1).conceptCount()).isEqualTo(1);
        assertThat(lessons.get(1).quizCount()).isEqualTo(1);
    }

    @Test
    void 계약11_countPublicLessons_코스별_공개_수업_수() {
        Map<Long, Integer> counts = courseLessonInfo.countPublicLessons(List.of(COURSE_A, 99999L));

        assertThat(counts.get(COURSE_A)).isEqualTo(2);
        assertThat(counts).doesNotContainKey(99999L);
    }

    @Test
    void 계약12_hasActiveEnrollment_진행_중인_수강이_있으면_true() {
        assertThat(loginEnrollmentChecker.hasActiveEnrollment(LEARNER)).isFalse();

        Enrollment enrollment = enroll(LEARNER, COURSE_A);
        assertThat(loginEnrollmentChecker.hasActiveEnrollment(LEARNER)).isTrue();

        enrollment.complete();
        em.flush();
        assertThat(loginEnrollmentChecker.hasActiveEnrollment(LEARNER)).isFalse();
    }

    private Enrollment enroll(long memberId, long courseId) {
        return enrollmentRepository.saveAndFlush(Enrollment.create(memberId, courseId, LocalDateTime.now()));
    }

    private void unlockActivity(Enrollment enrollment, long activityId) {
        activityProgressRepository.saveAndFlush(
                ActivityProgress.create(enrollment.getEnrollmentId(), activityId, LocalDateTime.now()));
    }

    private void unlockLesson(Enrollment enrollment, long lessonId) {
        lessonProgressRepository.saveAndFlush(
                LessonProgress.create(enrollment.getEnrollmentId(), lessonId, LocalDateTime.now()));
    }

    private void completeLesson(Enrollment enrollment, long lessonId) {
        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getEnrollmentId(), lessonId)
                .orElseGet(() -> lessonProgressRepository.save(
                        LessonProgress.create(enrollment.getEnrollmentId(), lessonId, LocalDateTime.now())));
        progress.complete();
        em.flush();
    }

    private ActivityProgress activityProgress(Enrollment enrollment, long activityId) {
        return activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), activityId)
                .orElseThrow();
    }

    private LessonProgress lessonProgress(Enrollment enrollment, long lessonId) {
        return lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollment.getEnrollmentId(), lessonId)
                .orElseThrow();
    }
}
