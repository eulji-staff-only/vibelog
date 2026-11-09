package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.LessonPublishStatus;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 운영자 수업 공개·비공개(API-ADM-32·33, FR-LSN-014, PL-LSN-003, PL-ENR-005, #118)를
 * 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다. 끝나면 모두 롤백된다(@Transactional).
 *
 * 테스트 코스 구조 (코스 9501)
 *  - 수업 1(9501, 공개) : 영향 학습자 수(API-ADM-32) 확인용. 수강 4명 — 진행 중(A)·완료(B)·포기(C)는 이 수업
 *    진도(lesson_progress)가 있고, 진행 중인 D는 같은 코스를 수강 중이지만 이 수업 진도가 아직 없다.
 *    완료·포기한 수강은 진도가 있어도, 진도가 없는 수강은 수강 중이어도 영향 학습자 수에서 빠져야 한다
 *  - 수업 2(9502, 비공개): 개념(9511) → 퀴즈(9512, 문항 3개), 모의고사 문항 1개 → 공개 가능
 *  - 수업 3(9503, 비공개): 개념(9521) → 퀴즈(9522, 문항 2개), 모의고사 문항 1개 → 퀴즈 문항 수만 모자람
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES "
                + "(9501, 'lsn-visibility-a@vibelog.test', '학습자A'), "
                + "(9502, 'lsn-visibility-b@vibelog.test', '학습자B'), "
                + "(9503, 'lsn-visibility-c@vibelog.test', '학습자C'), "
                + "(9504, 'lsn-visibility-d@vibelog.test', '학습자D')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9501, '테스트 트랙', '대상', '설명', 9501)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9501, '테스트 카테고리', 9501)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9501, 9501, 9501, '코스 9501', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9501, 9501, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9502, 9501, '수업 2', '소개', '목표', '본문', 20, 2, FALSE), "
                + "(9503, 9501, '수업 3', '소개', '목표', '본문', 10, 3, FALSE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, sort_order, is_active) VALUES "
                + "(9511, 9502, 'CONCEPT', '개념 2-1', '본문', 'MARKDOWN', 10, 1, TRUE), "
                + "(9512, 9502, 'QUIZ', '퀴즈 2-1', NULL, NULL, 10, 2, TRUE), "
                + "(9521, 9503, 'CONCEPT', '개념 3-1', '본문', 'MARKDOWN', 10, 1, TRUE), "
                + "(9522, 9503, 'QUIZ', '퀴즈 3-1', NULL, NULL, 10, 2, TRUE)",
        "INSERT INTO concept_tag (concept_tag_id, name) VALUES (9501, '테스트 태그')",
        "INSERT INTO quiz_question (quiz_question_id, activity_id, concept_tag_id, display_type, content, explanation, sort_order, is_active) VALUES "
                + "(9511, 9512, 9501, 'CONCEPT', '문항1', '해설', 1, TRUE), "
                + "(9512, 9512, 9501, 'CONCEPT', '문항2', '해설', 2, TRUE), "
                + "(9513, 9512, 9501, 'CONCEPT', '문항3', '해설', 3, TRUE), "
                + "(9521, 9522, 9501, 'CONCEPT', '문항1', '해설', 1, TRUE), "
                + "(9522, 9522, 9501, 'CONCEPT', '문항2', '해설', 2, TRUE)",
        "INSERT INTO exam_question (exam_question_id, lesson_id, concept_activity_id, content, explanation, sort_order, is_active) VALUES "
                + "(9501, 9502, 9511, '모의고사 문항', '해설', 1, TRUE), "
                + "(9502, 9503, 9521, '모의고사 문항', '해설', 1, TRUE)",
        "INSERT INTO exam_choice (exam_choice_id, exam_question_id, content, is_correct, sort_order) VALUES "
                + "(9501, 9501, '보기1', TRUE, 1), (9502, 9501, '보기2', FALSE, 2), "
                + "(9503, 9502, '보기1', TRUE, 1), (9504, 9502, '보기2', FALSE, 2)",
        "INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at, completed_at, dropped_at) VALUES "
                + "(9501, 9501, 9501, NOW(), NULL, NULL), "
                + "(9502, 9502, 9501, NOW(), NOW(), NULL), "
                + "(9503, 9503, 9501, NOW(), NULL, NOW()), "
                + "(9504, 9504, 9501, NOW(), NULL, NULL)",
        "INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at) VALUES "
                + "(9501, 9501, '2026-01-01 00:00:00'), (9502, 9501, '2026-01-01 00:00:00'), "
                + "(9503, 9501, '2026-01-01 00:00:00'), (9501, 9502, '2026-01-01 00:00:00')"
})
class LessonVisibilityServiceTest {

    static final long COURSE = 9501L;
    static final long LESSON_ALREADY_PUBLIC = 9501L;
    static final long LESSON_PUBLISHABLE = 9502L;
    static final long LESSON_TOO_FEW_QUESTIONS = 9503L;
    static final long ENROLLMENT_A = 9501L;
    static final long ENROLLMENT_B = 9502L;
    static final long ENROLLMENT_C = 9503L;

    @Autowired LessonAdminService lessonAdminService;
    @Autowired LessonRepository lessonRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;

    @Test
    void 공개_가능_조건을_모두_채우면_빠진_항목이_없다() {
        LessonPublishStatus status = lessonAdminService.checkPublishStatus(LESSON_PUBLISHABLE);

        assertThat(status.publishable()).isTrue();
        assertThat(status.missingRequirements()).isEmpty();
    }

    @Test
    void 퀴즈가_2문항뿐이면_공개되지_않고_퀴즈_3에서_5문항_항목이_빠진_것으로_보인다() {
        LessonPublishStatus status = lessonAdminService.checkPublishStatus(LESSON_TOO_FEW_QUESTIONS);

        assertThat(status.publishable()).isFalse();
        assertThat(status.missingRequirements()).contains("퀴즈 3~5문항");
        // 개념 뒤 퀴즈 순서와 모의고사 문항은 갖춰져 있어 이 항목들은 빠지지 않는다
        assertThat(status.missingRequirements()).doesNotContain("개념 뒤 퀴즈", "모의고사 문항");
    }

    @Test
    void 필수_항목이_빠진_수업을_공개하려_하면_빈_결과이고_비공개로_남는다() {
        assertThat(lessonAdminService.changeVisibility(LESSON_TOO_FEW_QUESTIONS, true)).isEmpty();
        assertThat(lessonRepository.findById(LESSON_TOO_FEW_QUESTIONS).orElseThrow().isPublic()).isFalse();
    }

    @Test
    void 공개하면_기존_수강_중_진도가_없는_수강에만_수업_진도가_새로_생기고_기존_진도는_그대로다() {
        lessonAdminService.changeVisibility(LESSON_PUBLISHABLE, true);

        assertThat(lessonRepository.findById(LESSON_PUBLISHABLE).orElseThrow().isPublic()).isTrue();

        // 이미 있던 진도(수강 A)는 unlocked_at이 그대로 유지된다
        LessonProgress existing = lessonProgressRepository.findByEnrollmentIdAndLessonId(ENROLLMENT_A, LESSON_PUBLISHABLE)
                .orElseThrow();
        assertThat(existing.getUnlockedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0, 0));

        // 진도가 없던 수강(B, C)에는 잠긴 채로 새로 생긴다
        List.of(ENROLLMENT_B, ENROLLMENT_C).forEach(enrollmentId -> {
            LessonProgress created = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, LESSON_PUBLISHABLE)
                    .orElseThrow();
            assertThat(created.isUnlocked()).isFalse();
        });
    }

    @Test
    void 공개된_수업을_비공개해도_필수_항목_검사_없이_바로_바뀐다() {
        lessonAdminService.changeVisibility(LESSON_ALREADY_PUBLIC, false);

        assertThat(lessonRepository.findById(LESSON_ALREADY_PUBLIC).orElseThrow().isPublic()).isFalse();
    }

    @Test
    void 영향_학습자_수는_이_수업_진도가_있으면서_완료_포기하지_않은_수강만_센다() {
        int activeLearnerCount = lessonAdminService.getActiveLearnerCount(LESSON_ALREADY_PUBLIC);

        assertThat(activeLearnerCount).isEqualTo(1);
    }
}
