package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dto.LessonActivityCard;
import com.vibelog.domain.lesson.model.dto.MyLearningLessonCard;
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
 * FR-EXM-006 지난 리포트 링크 값 — 내 학습(U-12)·수업 화면(U-13)이 계약 ④의 "최근 제출 회차"로 링크를 만드는지
 * 실제 DB로 확인한다. 테스트 데이터는 9750번대 ID로 넣고 끝나면 롤백된다.
 *
 * 회원 9750, 코스 9750
 *  - 수업 9750: 1회차 제출, 2회차 제출, 3회차 제출 안 함 → 링크는 attempt=2
 *  - 수업 9751: 응시 기록 없음 → 링크 없음
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES (9750, 'report-link@vibelog.test', '리포트링크')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9750, '테스트 트랙', '대상', '설명', 9750)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9750, '테스트 카테고리', 9750)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9750, 9750, 9750, '코스 9750', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9750, 9750, '수업 9750', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9751, 9750, '수업 9751', '소개', '목표', '본문', 30, 2, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, sort_order) VALUES "
                + "(9750, 9750, 'QUIZ', '퀴즈 9750', 1), (9751, 9751, 'QUIZ', '퀴즈 9751', 1)",
        "INSERT INTO exam_attempt (member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at) VALUES "
                + "(9750, 9750, 1, NOW(), 3, 60.00, NOW()), "
                + "(9750, 9750, 2, NOW(), 5, 100.00, NOW()), "
                + "(9750, 9750, 3, NOW(), NULL, NULL, NULL)"
})
class ReportLinkServiceTest {

    static final long MEMBER = 9750L;
    static final long COURSE = 9750L;
    static final long LESSON_SUBMITTED = 9750L;
    static final long LESSON_NONE = 9751L;

    @Autowired MyLearningService myLearningService;
    @Autowired LessonPageService lessonPageService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired EntityManager em;

    @Test
    void 내_학습은_제출한_회차가_있는_수업에만_가장_최근_제출_회차로_리포트_링크를_만든다() {
        openLessons();

        MyLearningLessonCard submitted = myLearningLesson(LESSON_SUBMITTED);
        MyLearningLessonCard none = myLearningLesson(LESSON_NONE);

        assertThat(submitted.reportUrl()).isEqualTo("/lessons/9750/exam/report?attempt=2"); // 제출 안 한 3회차는 제외
        assertThat(submitted.hasReport()).isTrue();
        assertThat(none.reportUrl()).isNull();
        assertThat(none.hasReport()).isFalse();
    }

    @Test
    void 수업_화면은_모의고사가_열렸고_제출한_회차가_있으면_지난_리포트_링크를_담는다() {
        openLessons();

        LessonActivityCard submitted = examCard(LESSON_SUBMITTED, false);
        LessonActivityCard none = examCard(LESSON_NONE, false);

        assertThat(submitted.reportUrl()).isEqualTo("/lessons/9750/exam/report?attempt=2");
        assertThat(none.reportUrl()).isNull();
    }

    @Test
    void 운영자_미리보기의_모의고사_카드에는_리포트_링크가_없다() {
        openLessons();

        assertThat(examCard(LESSON_SUBMITTED, true).reportUrl()).isNull();
    }

    private void openLessons() {
        Enrollment enrollment = enrollmentRepository.saveAndFlush(Enrollment.create(MEMBER, COURSE, LocalDateTime.now()));
        for (long lessonId : new long[]{LESSON_SUBMITTED, LESSON_NONE}) {
            lessonProgressRepository.saveAndFlush(
                    LessonProgress.create(enrollment.getEnrollmentId(), lessonId, LocalDateTime.now()));
        }
        for (long quizId : new long[]{9750L, 9751L}) { // 마지막 퀴즈 완료 → 모의고사 열림
            ActivityProgress progress = ActivityProgress.create(enrollment.getEnrollmentId(), quizId, LocalDateTime.now());
            progress.complete();
            activityProgressRepository.saveAndFlush(progress);
        }
        em.flush();
    }

    private MyLearningLessonCard myLearningLesson(long lessonId) {
        return myLearningService.getActiveCourses(MEMBER).get(0).lessons().stream()
                .filter(lesson -> lesson.lessonId() == lessonId).findFirst().orElseThrow();
    }

    private LessonActivityCard examCard(long lessonId, boolean preview) {
        return lessonPageService.getPage(MEMBER, COURSE, lessonId, null, preview).data().cards().stream()
                .filter(LessonActivityCard::isExam).findFirst().orElseThrow();
    }
}
