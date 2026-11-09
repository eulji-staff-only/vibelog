package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.exam.model.dto.ExamReportView;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/*
 * 오답 분석 리포트 (API-EXM-05, FR-EXM-003·004, PL-EXM-007, PL-LSN-008). 테스트가 끝나면 롤백된다.
 * 수업 완료·다음 수업 열림은 lesson 계약 ③ 실제 구현이 한다.
 *
 * 코스 9601: 수업 9601(개념 A 9601, 개념 B 9602) → 수업 9602(개념 9603).
 * 수업 9601 모의고사 문항: 9601·9602(개념 A), 9603(개념 B). 각 문항의 보기 1이 정답.
 * 회원 9601 의 1회차(제출): 9601 정답, 9602 오답, 9603 고르지 않음 → 1 / 3. 2회차는 제출 전.
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES (9601, 'report-a@vibelog.test', '리포트A'), (9602, 'report-b@vibelog.test', '리포트B')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9601, '리포트트랙', '대상', '설명', 9601)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9601, '리포트분류', 9601)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES (9601, 9601, 9601, '코스', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9601, 9601, '범위 정하기', '소개', '목표', '내용', 10, 1, TRUE), (9602, 9601, '바뀐 부분 확인', '소개', '목표', '내용', 10, 2, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9601, 9601, 'CONCEPT', '개념 A', '본문', 'MARKDOWN', 10, NULL, 1), (9602, 9601, 'CONCEPT', '개념 B', '본문', 'MARKDOWN', 10, NULL, 2), "
                + "(9603, 9602, 'CONCEPT', '다음 개념', '본문', 'MARKDOWN', 10, NULL, 1)",
        "INSERT INTO exam_question (exam_question_id, lesson_id, concept_activity_id, content, explanation, sort_order, is_active) VALUES "
                + "(9601, 9601, 9601, '문항1', '해설1', 1, TRUE), (9602, 9601, 9601, '문항2', '해설2', 2, TRUE), (9603, 9601, 9602, '문항3', '해설3', 3, TRUE)",
        "INSERT INTO exam_choice (exam_choice_id, exam_question_id, content, is_correct, sort_order) VALUES "
                + "(9611, 9601, '정답1', TRUE, 1), (9612, 9601, '오답1', FALSE, 2), (9621, 9602, '정답2', TRUE, 1), (9622, 9602, '오답2', FALSE, 2), "
                + "(9631, 9603, '정답3', TRUE, 1), (9632, 9603, '오답3', FALSE, 2)",
        "INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at) VALUES (9601, 9601, 9601, NOW())",
        "INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at) VALUES (9601, 9601, NOW())",
        "INSERT INTO exam_attempt (exam_attempt_id, member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at) VALUES "
                + "(9601, 9601, 9601, 1, NOW(), 1, 33.33, NOW()), (9602, 9601, 9601, 2, NOW(), NULL, NULL, NULL)",
        "INSERT INTO exam_answer (exam_attempt_id, exam_question_id, exam_choice_id, is_correct) VALUES "
                + "(9601, 9601, 9611, TRUE), (9601, 9602, 9622, FALSE), (9601, 9603, NULL, FALSE)"
})
class ExamReportServiceTest {

    static final Long MEMBER = 9601L;
    static final Long LESSON = 9601L;

    @Autowired ExamReportService examReportService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    private LocalDateTime reportViewedAt() {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT report_viewed_at FROM exam_attempt WHERE exam_attempt_id = 9601", LocalDateTime.class);
    }

    @Test
    void 회차가_없으면_최근_제출한_응시로_점수_개념별_정답률_틀린_문항을_보여_준다() {
        ExamReportView report = examReportService.getReport(MEMBER, LESSON, null).orElseThrow();

        assertThat(report.attemptNo()).isEqualTo(1);
        assertThat(report.lessonTitle()).isEqualTo("범위 정하기");
        assertThat(report.correctCount()).isEqualTo(1);
        assertThat(report.totalCount()).isEqualTo(3);
        assertThat(report.concepts()).extracting(ExamReportView.Concept::title, ExamReportView.Concept::rate, ExamReportView.Concept::weak)
                .containsExactly(tuple("개념 A", 50, true), tuple("개념 B", 0, true));
        assertThat(report.wrongs()).extracting(ExamReportView.Wrong::number, ExamReportView.Wrong::myChoice,
                        ExamReportView.Wrong::correctChoice, ExamReportView.Wrong::conceptTitle)
                .containsExactly(tuple(2, "오답2", "정답2", "개념 A"), tuple(3, null, "정답3", "개념 B"));
    }

    @Test
    void 처음_열면_연_일시를_남기고_수업을_완료해_다음_수업을_연다() {
        ExamReportView report = examReportService.getReport(MEMBER, LESSON, 1).orElseThrow();

        assertThat(reportViewedAt()).isNotNull();
        assertThat(report.nextLessonId()).isEqualTo(9602L);
        assertThat(report.courseId()).isEqualTo(9601L);
        assertThat(report.lastLesson()).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT completed_at IS NOT NULL FROM lesson_progress WHERE enrollment_id = 9601 AND lesson_id = 9601", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT unlocked_at IS NOT NULL FROM lesson_progress WHERE enrollment_id = 9601 AND lesson_id = 9602", Boolean.class)).isTrue();
    }

    @Test
    void 같은_응시를_다시_열어도_연_일시는_처음_그대로다() {
        examReportService.getReport(MEMBER, LESSON, 1);
        LocalDateTime first = reportViewedAt();
        entityManager.clear();

        examReportService.getReport(MEMBER, LESSON, 1);

        assertThat(reportViewedAt()).isEqualTo(first);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_progress WHERE enrollment_id = 9601 AND lesson_id = 9602", Integer.class)).isEqualTo(1);
    }

    @Test
    void 응시자가_5명_미만이면_평균을_보내지_않는다() {
        assertThat(examReportService.getReport(MEMBER, LESSON, 1).orElseThrow().averageScore()).isNull();
    }

    /*
     * 1회차 점수: 9601(기존) 1점 + 학습자 9611~9615 2·3·3·2·2점 → 6명 13점 ÷ 6 = 2.1666… → 2.2.
     * 운영자 9616·테스트 계정 9617(각 3점)과 9611 의 2회차(3점)는 넣지 않는다.
     */
    @Test
    void 평균은_학습자의_1회차_점수만_소수_첫째_자리로_계산한다() {
        jdbcTemplate.update("INSERT INTO member (member_id, email, nickname, role, is_test) VALUES "
                + "(9611, 'avg-1@vibelog.test', '평균1', 'LEARNER', FALSE), (9612, 'avg-2@vibelog.test', '평균2', 'LEARNER', FALSE), "
                + "(9613, 'avg-3@vibelog.test', '평균3', 'LEARNER', FALSE), (9614, 'avg-4@vibelog.test', '평균4', 'LEARNER', FALSE), "
                + "(9615, 'avg-5@vibelog.test', '평균5', 'LEARNER', FALSE), (9616, 'avg-admin@vibelog.test', '평균운영자', 'ADMIN', FALSE), "
                + "(9617, 'avg-test@vibelog.test', '평균테스트', 'LEARNER', TRUE)");
        jdbcTemplate.update("INSERT INTO exam_attempt (exam_attempt_id, member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at) VALUES "
                + "(9611, 9611, 9601, 1, NOW(), 2, 66.67, NOW()), (9612, 9612, 9601, 1, NOW(), 3, 100.00, NOW()), "
                + "(9613, 9613, 9601, 1, NOW(), 3, 100.00, NOW()), (9614, 9614, 9601, 1, NOW(), 2, 66.67, NOW()), "
                + "(9615, 9615, 9601, 1, NOW(), 2, 66.67, NOW()), (9616, 9616, 9601, 1, NOW(), 3, 100.00, NOW()), "
                + "(9617, 9617, 9601, 1, NOW(), 3, 100.00, NOW()), (9618, 9611, 9601, 2, NOW(), 3, 100.00, NOW())");

        assertThat(examReportService.getReport(MEMBER, LESSON, 1).orElseThrow().averageScore()).isEqualTo(2.2);
    }

    @Test
    void 제출_전_회차나_남의_응시는_리포트가_없다() {
        assertThat(examReportService.getReport(MEMBER, LESSON, 2)).isEmpty();
        assertThat(examReportService.getReport(9602L, LESSON, null)).isEmpty();
        assertThat(reportViewedAt()).isNull();
    }
}
