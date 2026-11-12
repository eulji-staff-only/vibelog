package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dto.CompleteActivityResult;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.domain.lesson.model.policy.ExamLessonProgress;
import com.vibelog.domain.lesson.model.policy.NextStep;
import com.vibelog.domain.lesson.model.policy.PortfolioAccess;
import com.vibelog.domain.lesson.model.policy.PortfolioEnrollmentProgress;
import com.vibelog.domain.lesson.model.policy.QuizActivityProgress;
import com.vibelog.domain.lesson.model.policy.ReportNextStep;
import com.vibelog.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 포기한 수강은 진행 기록(개념 완료·퀴즈 통과·모의고사 열림·리포트 확인·포트폴리오 열림·제출)을 바꾸지 못한다 (이슈 #306).
 * 수강 안 함과 같게 동작하고(잠김 결과·빈 값·ENROLLMENT_NOT_FOUND), 다시 시작하면 이어서 된다. 완료한 코스(복습)는 막지 않는다.
 * 실제 DB(01·02 실행된 로컬 MySQL)로 확인하고, 회원은 02-seed 테스트 계정(1 학습자)을 쓴다. 끝나면 모두 롤백된다.
 *
 * 테스트 코스 구조 (코스 9850)
 *  - 수업 1(9850, 공개): 개념(9850) → 퀴즈(9851)
 *  - 수업 2(9851, 공개, 마지막): 개념(9852) → 퀴즈(9853)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9850, '테스트 트랙', '대상', '설명', 9850)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9850, '테스트 카테고리', 9850)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9850, 9850, 9850, '코스 9850', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9850, 9850, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9851, 9850, '수업 2', '소개', '목표', '본문', 20, 2, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9850, 9850, 'CONCEPT', '개념 1', '본문', 'MARKDOWN', 10, NULL, 1), "
                + "(9851, 9850, 'QUIZ', '퀴즈 1', NULL, NULL, 10, 50, 2), "
                + "(9852, 9851, 'CONCEPT', '개념 2', '본문', 'MARKDOWN', 10, NULL, 1), "
                + "(9853, 9851, 'QUIZ', '퀴즈 2', NULL, NULL, 10, 50, 2)"
})
class DroppedEnrollmentProgressTest {

    static final long LEARNER = 1L;
    static final long COURSE = 9850L;
    static final long LESSON_1 = 9850L;
    static final long LESSON_2 = 9851L;
    static final long CONCEPT_1 = 9850L;
    static final long QUIZ_1 = 9851L;

    @Autowired LessonActivityService lessonActivityService;
    @Autowired CourseCompleteService courseCompleteService;
    @Autowired QuizActivityProgress quizActivityProgress;
    @Autowired ExamLessonProgress examLessonProgress;
    @Autowired PortfolioEnrollmentProgress portfolioEnrollmentProgress;

    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired EntityManager em;

    @Test
    void 포기한_수강은_개념을_완료해도_잠김으로_돌려주고_기록하지_않는다() {
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, CONCEPT_1);
        enrollment.drop();
        em.flush();

        CompleteActivityResult result = lessonActivityService.completeConcept(LEARNER, COURSE, LESSON_1, CONCEPT_1);
        em.flush();
        em.clear();

        assertThat(result.locked()).isTrue();
        assertThat(activityProgress(enrollment, CONCEPT_1).isCompleted()).isFalse();
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), QUIZ_1))
                .isEmpty();
    }

    @Test
    void 포기했다가_다시_시작하면_개념_완료가_이어서_된다() {
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, CONCEPT_1);
        enrollment.drop();
        em.flush();
        assertThat(lessonActivityService.completeConcept(LEARNER, COURSE, LESSON_1, CONCEPT_1).locked()).isTrue();

        enrollment.restart();
        em.flush();
        CompleteActivityResult result = lessonActivityService.completeConcept(LEARNER, COURSE, LESSON_1, CONCEPT_1);
        em.flush();

        assertThat(result.locked()).isFalse();
        assertThat(activityProgress(enrollment, CONCEPT_1).isCompleted()).isTrue();
        assertThat(activityProgress(enrollment, QUIZ_1).isUnlocked()).isTrue();
    }

    @Test
    void 포기한_수강은_퀴즈가_잠겨_있고_통과를_기록하지_못한다() {
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, QUIZ_1);
        assertThat(quizActivityProgress.getQuizActivity(LEARNER, QUIZ_1).unlocked()).isTrue();
        enrollment.drop();
        em.flush();

        assertThat(quizActivityProgress.getQuizActivity(LEARNER, QUIZ_1).unlocked()).isFalse();
        assertThatThrownBy(() -> quizActivityProgress.recordQuizPassed(LEARNER, QUIZ_1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.ENROLLMENT_NOT_FOUND);
        em.clear();
        assertThat(activityProgress(enrollment, QUIZ_1).isCompleted()).isFalse();
    }

    @Test
    void 다시_시작하면_퀴즈가_다시_열리고_통과를_기록한다() {
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, QUIZ_1);
        enrollment.drop();
        em.flush();
        enrollment.restart();
        em.flush();

        assertThat(quizActivityProgress.getQuizActivity(LEARNER, QUIZ_1).unlocked()).isTrue();
        NextStep step = quizActivityProgress.recordQuizPassed(LEARNER, QUIZ_1);

        assertThat(step.type()).isEqualTo(NextStep.EXAM);
        assertThat(activityProgress(enrollment, QUIZ_1).isCompleted()).isTrue();
    }

    @Test
    void 포기한_수강은_모의고사가_열려_있지_않고_다시_시작하면_열린다() {
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, QUIZ_1);
        activityProgress(enrollment, QUIZ_1).complete();
        em.flush();
        assertThat(examLessonProgress.isExamOpen(LEARNER, LESSON_1)).isTrue();

        enrollment.drop();
        em.flush();
        assertThat(examLessonProgress.isExamOpen(LEARNER, LESSON_1)).isFalse();

        enrollment.restart();
        em.flush();
        assertThat(examLessonProgress.isExamOpen(LEARNER, LESSON_1)).isTrue();
    }

    @Test
    void 포기한_수강은_리포트_확인을_기록하지_못하고_다음_수업도_열리지_않는다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment, LESSON_1);
        enrollment.drop();
        em.flush();

        assertThatThrownBy(() -> examLessonProgress.recordReportViewed(LEARNER, LESSON_1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.ENROLLMENT_NOT_FOUND);
        em.clear();
        assertThat(lessonProgress(enrollment, LESSON_1).isCompleted()).isFalse();
        assertThat(lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollment.getEnrollmentId(), LESSON_2))
                .isEmpty();
    }

    @Test
    void 다시_시작하면_리포트_확인이_기록되고_다음_수업이_열린다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment, LESSON_1);
        enrollment.drop();
        em.flush();
        enrollment.restart();
        em.flush();

        ReportNextStep step = examLessonProgress.recordReportViewed(LEARNER, LESSON_1);
        em.flush();

        assertThat(step.type()).isEqualTo(ReportNextStep.NEXT_LESSON);
        assertThat(lessonProgress(enrollment, LESSON_1).isCompleted()).isTrue();
        assertThat(lessonProgress(enrollment, LESSON_2).isUnlocked()).isTrue();
    }

    @Test
    void 포기한_수강은_마지막_리포트를_확인해도_포트폴리오가_열리지_않는다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment, LESSON_2);
        enrollment.drop();
        em.flush();

        assertThatThrownBy(() -> examLessonProgress.recordReportViewed(LEARNER, LESSON_2))
                .isInstanceOf(BusinessException.class);
        em.clear();
        assertThat(enrollmentRepository.findById(enrollment.getEnrollmentId()).orElseThrow().isPortfolioUnlocked())
                .isFalse();
    }

    @Test
    void 포기한_수강은_포트폴리오가_열려_있지_않고_제출을_기록하지_못한다() {
        Enrollment enrollment = enroll();
        completeLesson(enrollment, LESSON_1);
        completeLesson(enrollment, LESSON_2);
        enrollment.unlockPortfolio();
        enrollment.drop();
        em.flush();

        assertThat(portfolioEnrollmentProgress.getAccess(LEARNER, COURSE)).isEqualTo(new PortfolioAccess(false, false));
        assertThatThrownBy(() -> portfolioEnrollmentProgress.recordSubmitted(LEARNER, COURSE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.ENROLLMENT_NOT_FOUND);
        em.clear();
        assertThat(enrollmentRepository.findById(enrollment.getEnrollmentId()).orElseThrow().isCompleted()).isFalse();
    }

    @Test
    void 다시_시작하면_포트폴리오가_열리고_제출로_코스가_완료된다() {
        Enrollment enrollment = enroll();
        completeLesson(enrollment, LESSON_1);
        completeLesson(enrollment, LESSON_2);
        enrollment.unlockPortfolio();
        enrollment.drop();
        em.flush();
        enrollment.restart();
        em.flush();

        assertThat(portfolioEnrollmentProgress.getAccess(LEARNER, COURSE)).isEqualTo(new PortfolioAccess(true, true));
        portfolioEnrollmentProgress.recordSubmitted(LEARNER, COURSE);

        assertThat(enrollment.isCompleted()).isTrue();
    }

    @Test
    void 포기한_수강이어도_포트폴리오를_삭제하면_완료_기록은_비운다() {
        Enrollment enrollment = enroll();
        enrollment.complete();
        enrollment.drop();
        em.flush();

        portfolioEnrollmentProgress.recordDeleted(LEARNER, COURSE);

        assertThat(enrollment.isCompleted()).isFalse();
    }

    @Test
    void 포기한_수강은_코스_완료_화면_데이터가_비어_있고_다시_시작하면_보인다() {
        Enrollment enrollment = enroll();
        enrollment.drop();
        em.flush();

        assertThat(courseCompleteService.getCourseComplete(LEARNER, COURSE)).isEmpty();

        enrollment.restart();
        em.flush();
        assertThat(courseCompleteService.getCourseComplete(LEARNER, COURSE)).isPresent();
    }

    @Test
    void 완료한_코스는_포기하지_않았으면_복습이_계속_된다() {
        Enrollment enrollment = enroll();
        unlockActivity(enrollment, CONCEPT_1);
        unlockActivity(enrollment, QUIZ_1);
        activityProgress(enrollment, QUIZ_1).complete();
        enrollment.unlockPortfolio();
        enrollment.complete();
        em.flush();

        assertThat(lessonActivityService.completeConcept(LEARNER, COURSE, LESSON_1, CONCEPT_1).locked()).isFalse();
        assertThat(quizActivityProgress.getQuizActivity(LEARNER, QUIZ_1).unlocked()).isTrue();
        assertThat(examLessonProgress.isExamOpen(LEARNER, LESSON_1)).isTrue();
        assertThat(portfolioEnrollmentProgress.getAccess(LEARNER, COURSE)).isEqualTo(new PortfolioAccess(true, true));
        assertThat(courseCompleteService.getCourseComplete(LEARNER, COURSE)).isPresent();
    }

    private Enrollment enroll() {
        return enrollmentRepository.saveAndFlush(Enrollment.create(LEARNER, COURSE, LocalDateTime.now()));
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
        LessonProgress progress = lessonProgressRepository.save(
                LessonProgress.create(enrollment.getEnrollmentId(), lessonId, LocalDateTime.now()));
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
