package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
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
 *  - 수업 1(9501, 공개) : 영향 학습자 수(API-ADM-32) 확인용. 수강 5명, 전부 이 수업에 lesson_progress 행이
 *    있다(공개 수업은 수강 시작 때 전부 미리 만들어지므로, 행이 "있다"는 것만으로는 "열어봤다"를 뜻하지 않는다).
 *    A는 열어봤고 진행 중(수강도 진행 중) → 센다. B는 수강 완료(이 수업도 완료) → 수강이 끝나서 안 센다.
 *    C는 수강 포기(이 수업은 열어봄) → 수강이 끝나서 안 센다. D는 수강 중이지만 이 수업을 아직 안 열어봄
 *    (unlocked_at NULL) → 안 센다. E는 수강 중이고 이 수업도 열었지만 이미 끝냄(completed_at 있음) → 안 센다.
 *    A만 세어져야 한다(1명)
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
                + "(9504, 'lsn-visibility-d@vibelog.test', '학습자D'), "
                + "(9505, 'lsn-visibility-e@vibelog.test', '학습자E')",
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
                + "(9504, 9504, 9501, NOW(), NULL, NULL), "
                + "(9505, 9505, 9501, NOW(), NULL, NULL)",
        "INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at, completed_at) VALUES "
                + "(9501, 9501, '2026-01-01 00:00:00', NULL), "
                + "(9502, 9501, '2026-01-01 00:00:00', '2026-01-02 00:00:00'), "
                + "(9503, 9501, '2026-01-01 00:00:00', NULL), "
                + "(9504, 9501, NULL, NULL), "
                + "(9505, 9501, '2026-01-01 00:00:00', '2026-01-02 00:00:00'), "
                + "(9501, 9502, '2026-01-01 00:00:00', NULL)"
})
class LessonVisibilityServiceTest {

    static final long COURSE = 9501L;
    static final long LESSON_ALREADY_PUBLIC = 9501L;
    static final long LESSON_PUBLISHABLE = 9502L;
    static final long LESSON_TOO_FEW_QUESTIONS = 9503L;
    static final long ENROLLMENT_A = 9501L;
    static final long ENROLLMENT_B = 9502L;
    static final long ENROLLMENT_C = 9503L;
    static final long ENROLLMENT_D = 9504L;
    static final long ENROLLMENT_E = 9505L;
    static final long FIRST_CONCEPT = 9511L;   // 수업 2(공개 대상)의 첫 개념

    @Autowired LessonAdminService lessonAdminService;
    @Autowired LessonRepository lessonRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired EnrollmentRepository enrollmentRepository;

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
    void 공개하면_진도가_없던_수강에만_새로_생기고_이미_있던_진도는_그대로다() {
        lessonAdminService.changeVisibility(LESSON_PUBLISHABLE, true);

        assertThat(lessonRepository.findById(LESSON_PUBLISHABLE).orElseThrow().isPublic()).isTrue();

        // 이미 있던 진도(수강 A)는 unlocked_at이 그대로 유지된다
        LessonProgress existing = lessonProgressRepository.findByEnrollmentIdAndLessonId(ENROLLMENT_A, LESSON_PUBLISHABLE)
                .orElseThrow();
        assertThat(existing.getUnlockedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0, 0));

        // 나머지 수강(B~E)에는 진도가 새로 생긴다
        List.of(ENROLLMENT_B, ENROLLMENT_C, ENROLLMENT_D, ENROLLMENT_E).forEach(enrollmentId ->
                assertThat(lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, LESSON_PUBLISHABLE)).isPresent());
    }

    @Test
    void 이미_완료한_코스에_새_수업을_공개하면_완료는_그대로이고_새_수업과_첫_개념이_열린다() {
        // PL-ENR-005: 이미 완료한 코스는 새 수업이 추가되어도 완료 상태를 유지하고, 새 수업은 열어 둔다 (수강 B)
        lessonAdminService.changeVisibility(LESSON_PUBLISHABLE, true);

        assertThat(enrollmentRepository.findById(ENROLLMENT_B).orElseThrow().isCompleted()).isTrue();
        assertThat(lessonProgressRepository.findByEnrollmentIdAndLessonId(ENROLLMENT_B, LESSON_PUBLISHABLE)
                .orElseThrow().isUnlocked()).isTrue();
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(ENROLLMENT_B, FIRST_CONCEPT)
                .orElseThrow().isUnlocked()).isTrue();
    }

    @Test
    void 앞_수업을_이미_끝낸_진행_중_수강은_새_수업이_열리고_아직_못_끝낸_수강은_잠긴_채로_남는다() {
        // E: 수업 1을 이미 끝냄 → 다음 수업을 여는 계약 ③은 처음 끝낼 때뿐이라 지금 열어 주지 않으면 영영 잠긴다 (#357)
        // C(포기), D: 수업 1을 아직 못 끝냄 → 끝낼 때 계약 ③이 열어 주므로 잠긴 채로 둔다
        lessonAdminService.changeVisibility(LESSON_PUBLISHABLE, true);

        assertThat(lessonProgressRepository.findByEnrollmentIdAndLessonId(ENROLLMENT_E, LESSON_PUBLISHABLE)
                .orElseThrow().isUnlocked()).isTrue();
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(ENROLLMENT_E, FIRST_CONCEPT)
                .orElseThrow().isUnlocked()).isTrue();

        List.of(ENROLLMENT_C, ENROLLMENT_D).forEach(enrollmentId -> {
            assertThat(lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, LESSON_PUBLISHABLE)
                    .orElseThrow().isUnlocked()).isFalse();
            assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, FIRST_CONCEPT)).isEmpty();
        });
    }

    @Test
    void 공개된_수업을_비공개해도_필수_항목_검사_없이_바로_바뀐다() {
        lessonAdminService.changeVisibility(LESSON_ALREADY_PUBLIC, false);

        assertThat(lessonRepository.findById(LESSON_ALREADY_PUBLIC).orElseThrow().isPublic()).isFalse();
    }

    @Test
    void 영향_학습자_수는_이_수업을_열었고_아직_끝내지_않았고_수강도_끝나지_않은_학습자만_센다() {
        int activeLearnerCount = lessonAdminService.getActiveLearnerCount(LESSON_ALREADY_PUBLIC);

        assertThat(activeLearnerCount).isEqualTo(1);
    }
}
