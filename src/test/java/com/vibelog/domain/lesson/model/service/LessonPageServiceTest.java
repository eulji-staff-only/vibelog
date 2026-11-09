package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dto.ActivityStatus;
import com.vibelog.domain.lesson.model.dto.LessonActivityCard;
import com.vibelog.domain.lesson.model.dto.LessonPageData;
import com.vibelog.domain.lesson.model.dto.LessonPageResult;
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

import static org.assertj.core.api.Assertions.assertThat;

/*
 * API-LSN-01 수업 화면 데이터를 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 회원은 02-seed 테스트 계정(1 학습자)을 쓰고, 코스·수업·활동은 테스트 안에서만 넣는다. 끝나면 모두 롤백된다.
 *
 * 테스트 코스 구조 (코스 9101)
 *  - 수업 1(9101, 공개): 개념1(9101, 태그 2개) → 퀴즈1(9102) → 개념2(9103) → 퀴즈2(9104, 마지막 퀴즈)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9101, '테스트 트랙', '대상', '설명', 9101)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9101, '테스트 카테고리', 9101)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9101, 9101, 9101, '코스 9101', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9101, 9101, '수업 1', '소개', '목표', '본문', 30, 1, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, tool_note_url, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9101, 9101, 'CONCEPT', '개념 1', '첫 줄\n둘째 줄', 'MARKDOWN', 'https://notion.so/concept-1', 10, NULL, 1), "
                + "(9102, 9101, 'QUIZ', '퀴즈 1', NULL, NULL, NULL, 10, 50, 2), "
                + "(9103, 9101, 'CONCEPT', '개념 2', '본문', 'MARKDOWN', NULL, 10, NULL, 3), "
                + "(9104, 9101, 'QUIZ', '퀴즈 2', NULL, NULL, NULL, 10, 50, 4)",
        "INSERT INTO concept_tag (concept_tag_id, name) VALUES (9101, '변수'), (9102, '자료형')",
        "INSERT INTO activity_tag (activity_id, concept_tag_id) VALUES (9101, 9101), (9101, 9102)"
})
class LessonPageServiceTest {

    static final long LEARNER = 1L;
    static final long COURSE = 9101L;
    static final long LESSON = 9101L;
    static final long CONCEPT_1 = 9101L;
    static final long QUIZ_1 = 9102L;
    static final long CONCEPT_2 = 9103L;
    static final long QUIZ_2 = 9104L;

    @Autowired LessonPageService lessonPageService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired EntityManager em;

    @Test
    void 수강하지_않았으면_NOT_ENROLLED() {
        LessonPageResult result = lessonPageService.getPage(LEARNER, COURSE, LESSON, null);

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.NOT_ENROLLED);
    }

    @Test
    void 수강을_포기했으면_열려_있던_수업도_NOT_ENROLLED() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment);
        enrollment.drop();
        enrollmentRepository.saveAndFlush(enrollment);

        LessonPageResult result = lessonPageService.getPage(LEARNER, COURSE, LESSON, null);

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.NOT_ENROLLED);
    }

    @Test
    void 포기한_코스를_다시_시작하면_수업_화면이_다시_열린다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment);
        enrollment.drop();
        enrollmentRepository.saveAndFlush(enrollment);
        enrollment.restart();
        enrollmentRepository.saveAndFlush(enrollment);

        LessonPageResult result = lessonPageService.getPage(LEARNER, COURSE, LESSON, null);

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.OK);
    }

    @Test
    void 완료한_코스는_복습할_수_있어_수업_화면이_열린다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment);
        enrollment.complete();
        enrollmentRepository.saveAndFlush(enrollment);

        LessonPageResult result = lessonPageService.getPage(LEARNER, COURSE, LESSON, null);

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.OK);
    }

    @Test
    void 수강은_했지만_수업이_안_열렸으면_LOCKED() {
        enroll();

        LessonPageResult result = lessonPageService.getPage(LEARNER, COURSE, LESSON, null);

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.LOCKED);
    }

    @Test
    void 열린_수업이면_활동_카드를_만들고_진행_중인_활동을_기본으로_고른다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment);
        completeActivity(enrollment, CONCEPT_1);
        unlockActivity(enrollment, QUIZ_1); // 진행 중

        LessonPageResult result = lessonPageService.getPage(LEARNER, COURSE, LESSON, null);
        LessonPageData data = result.data();

        assertThat(result.status()).isEqualTo(LessonPageResult.Status.OK);
        assertThat(data.cards()).hasSize(5); // 개념2 · 퀴즈2 · 모의고사까지 포함

        LessonActivityCard concept1 = cardOf(data, CONCEPT_1);
        assertThat(concept1.status()).isEqualTo(ActivityStatus.COMPLETED);
        assertThat(concept1.conceptTags()).containsExactlyInAnyOrder("변수", "자료형");
        assertThat(concept1.toolNoteUrl()).isEqualTo("https://notion.so/concept-1");
        assertThat(concept1.body()).contains("<br>"); // 마크다운 줄바꿈 최소 처리
        assertThat(concept1.selected()).isFalse();

        LessonActivityCard quiz1 = cardOf(data, QUIZ_1);
        assertThat(quiz1.status()).isEqualTo(ActivityStatus.IN_PROGRESS);
        assertThat(quiz1.bestCorrectRate()).isNull(); // Fake 는 항상 null (quiz 영역 생기기 전)
        assertThat(quiz1.selected()).isTrue(); // 기본 선택

        LessonActivityCard concept2 = cardOf(data, CONCEPT_2);
        assertThat(concept2.status()).isEqualTo(ActivityStatus.LOCKED);
        assertThat(concept2.previousActivityId()).isEqualTo(QUIZ_1);

        LessonActivityCard exam = examCard(data);
        assertThat(exam.status()).isEqualTo(ActivityStatus.LOCKED);
        assertThat(exam.previousActivityId()).isEqualTo(QUIZ_2);
    }

    @Test
    void activity_쿼리로_선택한_활동을_바꿀_수_있다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment);
        completeActivity(enrollment, CONCEPT_1);
        unlockActivity(enrollment, QUIZ_1);

        LessonPageData data = lessonPageService.getPage(LEARNER, COURSE, LESSON, String.valueOf(CONCEPT_1)).data();

        assertThat(cardOf(data, CONCEPT_1).selected()).isTrue();
        assertThat(cardOf(data, QUIZ_1).selected()).isFalse();
    }

    @Test
    void 알_수_없는_activity_쿼리는_무시하고_기본값을_쓴다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment);
        completeActivity(enrollment, CONCEPT_1);
        unlockActivity(enrollment, QUIZ_1);

        LessonPageData data = lessonPageService.getPage(LEARNER, COURSE, LESSON, "no-such-activity").data();

        assertThat(cardOf(data, QUIZ_1).selected()).isTrue();
    }

    @Test
    void 모든_활동을_마치면_기본_선택이_모의고사로_바뀌고_모의고사가_열린다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment);
        completeActivity(enrollment, CONCEPT_1);
        completeActivity(enrollment, QUIZ_1);
        completeActivity(enrollment, CONCEPT_2);
        completeActivity(enrollment, QUIZ_2); // 마지막 퀴즈 완료 → 모의고사 열림(계약 ②)

        LessonPageData data = lessonPageService.getPage(LEARNER, COURSE, LESSON, null).data();

        LessonActivityCard exam = examCard(data);
        assertThat(exam.status()).isEqualTo(ActivityStatus.IN_PROGRESS);
        assertThat(exam.examUrl()).isEqualTo("/lessons/" + LESSON + "/exam/attempts");
        assertThat(exam.selected()).isTrue();
    }

    @Test
    void activity_exam_쿼리로_모의고사_카드를_바로_고를_수_있다() {
        Enrollment enrollment = enroll();
        unlockLesson(enrollment);
        completeActivity(enrollment, CONCEPT_1);
        unlockActivity(enrollment, QUIZ_1);

        LessonPageData data = lessonPageService.getPage(LEARNER, COURSE, LESSON, "exam").data();

        assertThat(examCard(data).selected()).isTrue();
    }

    private Enrollment enroll() {
        return enrollmentRepository.saveAndFlush(Enrollment.create(LEARNER, COURSE, LocalDateTime.now()));
    }

    private void unlockLesson(Enrollment enrollment) {
        lessonProgressRepository.saveAndFlush(LessonProgress.create(enrollment.getEnrollmentId(), LESSON, LocalDateTime.now()));
    }

    private void unlockActivity(Enrollment enrollment, long activityId) {
        activityProgressRepository.saveAndFlush(
                ActivityProgress.create(enrollment.getEnrollmentId(), activityId, LocalDateTime.now()));
    }

    private void completeActivity(Enrollment enrollment, long activityId) {
        ActivityProgress progress = activityProgressRepository
                .findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), activityId)
                .orElseGet(() -> activityProgressRepository.save(
                        ActivityProgress.create(enrollment.getEnrollmentId(), activityId, LocalDateTime.now())));
        progress.complete();
        em.flush();
    }

    private LessonActivityCard cardOf(LessonPageData data, long activityId) {
        return data.cards().stream()
                .filter(card -> Long.valueOf(activityId).equals(card.activityId()))
                .findFirst()
                .orElseThrow();
    }

    private LessonActivityCard examCard(LessonPageData data) {
        return data.cards().stream()
                .filter(card -> LessonActivityCard.EXAM.equals(card.kind()))
                .findFirst()
                .orElseThrow();
    }
}
