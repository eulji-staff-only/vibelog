package com.vibelog.domain.lesson.model.policy;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 모의고사 열림 규칙(PL-LSN-002 마지막 퀴즈 통과, PL-ENR-006 한 번 열린 것은 다시 잠그지 않는다)을
 * 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다. 회원은 02-seed 테스트 계정(1 학습자)을 쓰고 코스·수업·활동은 테스트 안에서만 넣는다.
 * 끝나면 모두 롤백된다.
 *
 * 테스트 코스 (9601)
 *  - 수업 1(9601): 개념(9611) → 퀴즈(9612) → 개념(9613) → 퀴즈(9614), 모두 2026-01-01에 만들어짐
 *  - 수업 2(9602): 위와 같은 4개(9621~9624) + 나중에 추가된 개념(9625) → 퀴즈(9626). 추가분은 created_at 2099-01-01
 *    (학습자가 퀴즈를 통과한 시각보다 늦다)
 *  - 수업 3(9603): 퀴즈 없음
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9601, '테스트 트랙', '대상', '설명', 9601)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9601, '테스트 카테고리', 9601)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9601, 9601, 9601, '코스 9601', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9601, 9601, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9602, 9601, '수업 2', '소개', '목표', '본문', 20, 2, TRUE), "
                + "(9603, 9601, '수업 3(퀴즈 없음)', '소개', '목표', '본문', 10, 3, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, sort_order, is_active, created_at) VALUES "
                + "(9611, 9601, 'CONCEPT', '개념 1-1', '본문', 'MARKDOWN', 10, 1, TRUE, '2026-01-01 00:00:00'), "
                + "(9612, 9601, 'QUIZ', '퀴즈 1-1', NULL, NULL, 10, 2, TRUE, '2026-01-01 00:00:00'), "
                + "(9613, 9601, 'CONCEPT', '개념 1-2', '본문', 'MARKDOWN', 10, 3, TRUE, '2026-01-01 00:00:00'), "
                + "(9614, 9601, 'QUIZ', '퀴즈 1-2', NULL, NULL, 10, 4, TRUE, '2026-01-01 00:00:00'), "
                + "(9621, 9602, 'CONCEPT', '개념 2-1', '본문', 'MARKDOWN', 10, 1, TRUE, '2026-01-01 00:00:00'), "
                + "(9622, 9602, 'QUIZ', '퀴즈 2-1', NULL, NULL, 10, 2, TRUE, '2026-01-01 00:00:00'), "
                + "(9623, 9602, 'CONCEPT', '개념 2-2', '본문', 'MARKDOWN', 10, 3, TRUE, '2026-01-01 00:00:00'), "
                + "(9624, 9602, 'QUIZ', '퀴즈 2-2', NULL, NULL, 10, 4, TRUE, '2026-01-01 00:00:00'), "
                + "(9625, 9602, 'CONCEPT', '나중에 추가한 개념', '본문', 'MARKDOWN', 10, 5, TRUE, '2099-01-01 00:00:00'), "
                + "(9626, 9602, 'QUIZ', '나중에 추가한 퀴즈', NULL, NULL, 10, 6, TRUE, '2099-01-01 00:00:00')"
})
class LessonExamOpenPolicyTest {

    static final long LEARNER = 1L;
    static final long COURSE = 9601L;

    @Autowired LessonExamOpenPolicy lessonExamOpenPolicy;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;

    private long enrollmentId;

    @BeforeEach
    void startEnrollment() {
        enrollmentId = enrollmentRepository.saveAndFlush(Enrollment.create(LEARNER, COURSE, LocalDateTime.now())).getEnrollmentId();
    }

    private void passQuiz(long activityId) {
        ActivityProgress progress = ActivityProgress.create(enrollmentId, activityId, LocalDateTime.now());
        progress.complete();
        activityProgressRepository.saveAndFlush(progress);
    }

    @Test
    void 퀴즈를_하나도_통과하지_않았으면_닫혀_있다() {
        assertThat(lessonExamOpenPolicy.isOpen(enrollmentId, 9601L)).isFalse();
    }

    @Test
    void 마지막_퀴즈를_통과하기_전에는_닫혀_있다() {
        passQuiz(9612L);

        assertThat(lessonExamOpenPolicy.isOpen(enrollmentId, 9601L)).isFalse();
    }

    @Test
    void 마지막_퀴즈를_통과하면_열린다() {
        passQuiz(9612L);
        passQuiz(9614L);

        assertThat(lessonExamOpenPolicy.isOpen(enrollmentId, 9601L)).isTrue();
    }

    @Test
    void 모의고사가_열린_뒤_수업_끝에_퀴즈를_추가해도_다시_잠그지_않는다() {
        // PL-ENR-006: 한 번 열린 것은 다시 잠그지 않는다. 지금 "마지막 퀴즈"(9626)는 통과하지 않았지만 열려 있어야 한다 (#357)
        passQuiz(9622L);
        passQuiz(9624L);

        assertThat(lessonExamOpenPolicy.isOpen(enrollmentId, 9602L)).isTrue();
    }

    @Test
    void 열리기_전에_이미_있던_퀴즈는_추가된_퀴즈가_있어도_그대로_통과해야_한다() {
        // 9624는 학습자가 9622를 통과하기 전에 이미 있던 퀴즈라서 통과해야 열린다 (PL-LSN-002)
        passQuiz(9622L);

        assertThat(lessonExamOpenPolicy.isOpen(enrollmentId, 9602L)).isFalse();
    }

    @Test
    void 퀴즈가_없는_수업은_열리지_않는다() {
        assertThat(lessonExamOpenPolicy.isOpen(enrollmentId, 9603L)).isFalse();
    }
}
