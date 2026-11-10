package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dto.LessonExamStatus;
import com.vibelog.domain.lesson.model.dto.LessonStatus;
import com.vibelog.domain.lesson.model.dto.MyLearningCourseCard;
import com.vibelog.domain.lesson.model.dto.MyLearningLessonCard;
import com.vibelog.domain.lesson.model.dto.PortfolioStatus;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 내 학습(U-12, API-ENR-02) 데이터 — 수업 상태 · 모의고사 상태 · 포트폴리오 상태 · 진행률(PL-ENR-006)을
 * 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다. 회원은 02-seed 테스트 계정(1 학습자)을 쓰고,
 * 코스·수업은 테스트 안에서만 넣는다. 끝나면 모두 롤백된다.
 *
 * 테스트 코스 구조 (코스 B, 9101) — 공개 수업 3개
 *  - 수업 1(9101): 완료 (리포트 확인까지 끝남)
 *  - 수업 2(9102): 열림, 모의고사 응시 안 함 → 진행 중 · 모의고사 열림
 *  - 수업 3(9103): 잠김 (진도 없음)
 *
 * 코스 C(9102)는 완료 탭 확인용 — 완료한 수강 하나만 둔다.
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9101, '테스트 트랙', '대상', '설명', 9101)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9101, '테스트 카테고리', 9101)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES "
                + "(9101, 9101, 9101, '코스 B', '요약', '설명', '대상', '목표', 7), "
                + "(9102, 9101, 9101, '코스 C', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9101, 9101, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9102, 9101, '수업 2', '소개', '목표', '본문', 20, 2, TRUE), "
                + "(9103, 9101, '수업 3', '소개', '목표', '본문', 10, 3, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, sort_order) VALUES "
                + "(9102, 9102, 'QUIZ', '수업 2 마지막 퀴즈', 1)"
})
class MyLearningServiceTest {

    static final long LEARNER = 1L;
    static final long COURSE_B = 9101L;
    static final long COURSE_C = 9102L;
    static final long LESSON_1 = 9101L;
    static final long LESSON_2 = 9102L;
    static final long LESSON_3 = 9103L;

    @Autowired MyLearningService myLearningService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired EntityManager em;

    @Test
    void 진행률은_리포트까지_확인한_수업_수_나누기_공개_수업_수다() {
        Enrollment enrollment = enroll(LEARNER, COURSE_B);
        completeLesson(enrollment, LESSON_1);
        unlockLesson(enrollment, LESSON_2);
        // 수업 3은 진도 자체가 없다 (잠김)

        List<MyLearningCourseCard> cards = myLearningService.getActiveCourses(LEARNER);

        assertThat(cards).hasSize(1);
        MyLearningCourseCard card = cards.get(0);
        // 완료 1개 ÷ 공개 수업 3개 = 33% (반올림)
        assertThat(card.progressPercent()).isEqualTo(33);
    }

    @Test
    void 카드에는_실제_트랙_이름과_코스_이름이_나온다() {
        // 임시 Fake 가 "샘플 트랙"·"샘플 코스 N"을 돌려주던 문제(#355)가 다시 생기면 실패한다
        enroll(LEARNER, COURSE_B);

        MyLearningCourseCard card = myLearningService.getActiveCourses(LEARNER).get(0);

        assertThat(card.trackName()).isEqualTo("테스트 트랙");
        assertThat(card.courseName()).isEqualTo("코스 B");
    }

    @Test
    void 수업_상태는_진도에_따라_잠김_진행중_완료로_나뉜다() {
        Enrollment enrollment = enroll(LEARNER, COURSE_B);
        completeLesson(enrollment, LESSON_1);
        unlockLesson(enrollment, LESSON_2);
        completeLastQuiz(enrollment, 9102L); // 마지막 퀴즈를 통과해야 모의고사가 열린다 (PL-LSN-002)

        MyLearningCourseCard card = myLearningService.getActiveCourses(LEARNER).get(0);
        MyLearningLessonCard lesson1 = findLesson(card, LESSON_1);
        MyLearningLessonCard lesson2 = findLesson(card, LESSON_2);
        MyLearningLessonCard lesson3 = findLesson(card, LESSON_3);

        assertThat(lesson1.status()).isEqualTo(LessonStatus.COMPLETED);
        assertThat(lesson1.examStatus()).isEqualTo(LessonExamStatus.REPORT_CONFIRMED);

        assertThat(lesson2.status()).isEqualTo(LessonStatus.IN_PROGRESS);
        // 제출한 모의고사 응시가 없으므로 "열림(응시 안 함)"
        assertThat(lesson2.examStatus()).isEqualTo(LessonExamStatus.OPEN);

        assertThat(lesson3.status()).isEqualTo(LessonStatus.LOCKED);
        assertThat(lesson3.examStatus()).isEqualTo(LessonExamStatus.LOCKED);
    }

    @Test
    void 수업이_열려도_마지막_퀴즈를_통과하기_전에는_모의고사가_잠긴다() {
        // PL-LSN-002: 마지막 퀴즈를 통과해야 모의고사가 열린다. 수업이 열려 있다고 바로 열리는 게 아니다.
        Enrollment enrollment = enroll(LEARNER, COURSE_B);
        unlockLesson(enrollment, LESSON_2);

        MyLearningCourseCard card = myLearningService.getActiveCourses(LEARNER).get(0);
        MyLearningLessonCard lesson2 = findLesson(card, LESSON_2);

        assertThat(lesson2.status()).isEqualTo(LessonStatus.IN_PROGRESS);
        assertThat(lesson2.examStatus()).isEqualTo(LessonExamStatus.LOCKED);
        assertThat(card.remainingConditions()).anyMatch(condition -> condition.contains("마지막 퀴즈를 통과하면"));
    }

    @Test
    void 포트폴리오가_잠겨_있으면_남은_완료_조건에_포함된다() {
        enroll(LEARNER, COURSE_B);

        MyLearningCourseCard card = myLearningService.getActiveCourses(LEARNER).get(0);

        assertThat(card.portfolioStatus()).isEqualTo(PortfolioStatus.LOCKED);
        assertThat(card.remainingConditions()).anyMatch(condition -> condition.contains("포트폴리오"));
        assertThat(card.nextStepText()).isNotBlank();
    }

    @Test
    void 완료_탭에는_완료한_코스만_보인다() {
        Enrollment active = enroll(LEARNER, COURSE_B);
        Enrollment completed = enroll(LEARNER, COURSE_C);
        completed.complete();
        em.flush();

        List<MyLearningCourseCard> activeCards = myLearningService.getActiveCourses(LEARNER);
        List<MyLearningCourseCard> completedCards = myLearningService.getCompletedCourses(LEARNER);

        assertThat(activeCards).extracting(MyLearningCourseCard::courseId).containsExactly(active.getCourseId());
        assertThat(completedCards).extracting(MyLearningCourseCard::courseId).containsExactly(completed.getCourseId());
        assertThat(completedCards.get(0).completed()).isTrue();
    }

    @Test
    void 포기한_코스는_완료_시각이_있어도_완료_탭에_나오지_않는다() {
        Enrollment completed = enroll(LEARNER, COURSE_C);
        completed.complete();
        Enrollment droppedThenCompleted = enroll(LEARNER, COURSE_B);
        droppedThenCompleted.drop();
        droppedThenCompleted.complete(); // 포기한 동안 포트폴리오를 제출해 완료 시각이 기록된 경우
        em.flush();

        assertThat(myLearningService.getCompletedCourses(LEARNER))
                .extracting(MyLearningCourseCard::courseId).containsExactly(completed.getCourseId());
        assertThat(myLearningService.getActiveCourses(LEARNER)).isEmpty();
    }

    private MyLearningLessonCard findLesson(MyLearningCourseCard card, long lessonId) {
        return card.lessons().stream().filter(l -> l.lessonId() == lessonId).findFirst().orElseThrow();
    }

    private Enrollment enroll(long memberId, long courseId) {
        return enrollmentRepository.saveAndFlush(Enrollment.create(memberId, courseId, LocalDateTime.now()));
    }

    private void unlockLesson(Enrollment enrollment, long lessonId) {
        lessonProgressRepository.saveAndFlush(
                LessonProgress.create(enrollment.getEnrollmentId(), lessonId, LocalDateTime.now()));
    }

    private void completeLesson(Enrollment enrollment, long lessonId) {
        LessonProgress progress = LessonProgress.create(enrollment.getEnrollmentId(), lessonId, LocalDateTime.now());
        progress.complete();
        lessonProgressRepository.saveAndFlush(progress);
    }

    private void completeLastQuiz(Enrollment enrollment, long activityId) {
        ActivityProgress progress = ActivityProgress.create(enrollment.getEnrollmentId(), activityId, LocalDateTime.now());
        progress.complete();
        activityProgressRepository.saveAndFlush(progress);
    }
}
