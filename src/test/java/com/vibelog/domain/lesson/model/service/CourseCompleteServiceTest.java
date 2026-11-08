package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dto.CourseCompleteCondition;
import com.vibelog.domain.lesson.model.dto.CourseCompleteView;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 코스 완료 화면(U-18, API-ENR-03) 데이터 — 완료 조건 3개(PL-ENR-003: 모의고사 응시 · 오답 분석 리포트 확인 ·
 * 포트폴리오 제출)의 완료·남음과 완료 일시, 수업별 리포트 목록을 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 회원은 02-seed 테스트 계정(1 학습자)을 쓰고, 코스·수업·모의고사 응시·포트폴리오는 테스트 안에서만 넣는다.
 * 끝나면 모두 롤백된다.
 *
 * 테스트 코스 구조 (코스, 9301) — 공개 수업 2개(9301, 9302)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9301, '테스트 트랙', '대상', '설명', 9301)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9301, '테스트 카테고리', 9301)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES "
                + "(9301, 9301, 9301, '테스트 코스', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9301, 9301, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9302, 9301, '수업 2', '소개', '목표', '본문', 20, 2, TRUE)"
})
class CourseCompleteServiceTest {

    static final long LEARNER = 1L;
    static final long COURSE = 9301L;
    static final long LESSON_1 = 9301L;
    static final long LESSON_2 = 9302L;

    @Autowired CourseCompleteService courseCompleteService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired EntityManager em;

    @Test
    void 완료_조건을_모두_채우면_완료_상태와_완료_일시를_보여준다() {
        Enrollment enrollment = enroll(LEARNER, COURSE);
        completeLesson(enrollment, LESSON_1);
        completeLesson(enrollment, LESSON_2);
        submitExam(LEARNER, LESSON_1, 1);
        submitExam(LEARNER, LESSON_2, 1);
        submitPortfolio(LEARNER, COURSE);
        enrollment.complete(); // 계약 ⑦이 이미 기록했다고 가정 (이 Service 는 읽기만 한다)
        em.flush();

        CourseCompleteView view = courseCompleteService.getCourseComplete(LEARNER, COURSE).orElseThrow();

        assertThat(view.completed()).isTrue();
        assertThat(view.completedAt()).isNotNull();
        assertThat(view.conditions()).allMatch(CourseCompleteCondition::met);
        assertThat(view.lessonReports()).hasSize(2)
                .allMatch(report -> report.reportUrl() != null);
    }

    @Test
    void 조건이_남으면_한_걸음_남았어요_상태와_남은_조건을_보여준다() {
        Enrollment enrollment = enroll(LEARNER, COURSE);
        completeLesson(enrollment, LESSON_1);
        unlockLesson(enrollment, LESSON_2); // 수업 2는 아직 리포트 확인 전
        submitExam(LEARNER, LESSON_1, 1);
        // 수업 2는 모의고사 응시 안 함, 포트폴리오도 제출 안 함

        CourseCompleteView view = courseCompleteService.getCourseComplete(LEARNER, COURSE).orElseThrow();

        assertThat(view.completed()).isFalse();
        assertThat(view.completedAt()).isNull();
        List<CourseCompleteCondition> conditions = view.conditions();
        assertThat(conditionNamed(conditions, "모든 공개 수업 모의고사 응시").met()).isFalse();
        assertThat(conditionNamed(conditions, "모든 공개 수업 오답 분석 리포트 확인").met()).isFalse();
        assertThat(conditionNamed(conditions, "포트폴리오 제출").met()).isFalse();
        // 응시하지 않은 수업은 리포트 링크가 없다
        assertThat(view.lessonReports()).anyMatch(report -> report.reportUrl() == null);
    }

    @Test
    void 포트폴리오를_삭제하면_완료가_취소되고_포트폴리오_조건만_다시_미충족이_된다() {
        // PL-ENR-003: 포트폴리오 삭제는 계약 ⑦(recordDeleted)이 enrollment.completed_at 을 비우고,
        // 이 테스트는 이미 비워진 상태(enrollment)와 삭제된 포트폴리오(deleted_at)를 두고 화면이
        // "완료 아님 + 한 걸음 남았어요"로 정확히 보이는지만 확인한다.
        Enrollment enrollment = enroll(LEARNER, COURSE);
        completeLesson(enrollment, LESSON_1);
        completeLesson(enrollment, LESSON_2);
        submitExam(LEARNER, LESSON_1, 1);
        submitExam(LEARNER, LESSON_2, 1);
        submitPortfolio(LEARNER, COURSE);
        deletePortfolio(LEARNER, COURSE);
        // enrollment.completed_at 은 비워진 채로 둔다 (complete() 를 부르지 않음 = cancelCompletion() 된 상태)

        CourseCompleteView view = courseCompleteService.getCourseComplete(LEARNER, COURSE).orElseThrow();

        assertThat(view.completed()).isFalse();
        assertThat(view.completedAt()).isNull();
        List<CourseCompleteCondition> conditions = view.conditions();
        assertThat(conditionNamed(conditions, "모든 공개 수업 모의고사 응시").met()).isTrue();
        assertThat(conditionNamed(conditions, "모든 공개 수업 오답 분석 리포트 확인").met()).isTrue();
        assertThat(conditionNamed(conditions, "포트폴리오 제출").met()).isFalse();
    }

    @Test
    void 수강하지_않았으면_빈_값을_돌려준다() {
        Optional<CourseCompleteView> view = courseCompleteService.getCourseComplete(LEARNER, COURSE);

        assertThat(view).isEmpty();
    }

    private CourseCompleteCondition conditionNamed(List<CourseCompleteCondition> conditions, String name) {
        return conditions.stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
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

    private void submitExam(long memberId, long lessonId, int attemptNo) {
        em.createNativeQuery("INSERT INTO exam_attempt (member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at) "
                        + "VALUES (:memberId, :lessonId, :attemptNo, NOW(), 5, 100.00, NOW())")
                .setParameter("memberId", memberId)
                .setParameter("lessonId", lessonId)
                .setParameter("attemptNo", attemptNo)
                .executeUpdate();
    }

    private void submitPortfolio(long memberId, long courseId) {
        em.createNativeQuery("INSERT INTO portfolio (member_id, course_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public) "
                        + "VALUES (:memberId, :courseId, 'COURSE', '제목', 'https://example.com', '요약', '회고', 'Claude', TRUE)")
                .setParameter("memberId", memberId)
                .setParameter("courseId", courseId)
                .executeUpdate();
    }

    private void deletePortfolio(long memberId, long courseId) {
        em.createNativeQuery("UPDATE portfolio SET deleted_at = NOW() WHERE member_id = :memberId AND course_id = :courseId")
                .setParameter("memberId", memberId)
                .setParameter("courseId", courseId)
                .executeUpdate();
    }
}
