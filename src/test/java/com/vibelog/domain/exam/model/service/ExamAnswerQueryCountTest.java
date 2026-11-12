package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.exam.model.dto.ExamReportView;
import com.vibelog.domain.exam.model.dto.ExamWeakConcept;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 모의고사 답 목록을 읽을 때 문항·보기를 답마다 따로 조회하지 않는지 확인한다 (#476, 점검표 P-1).
 * 영속성 컨텍스트를 비운 뒤(flush·clear) 호출해야 LAZY 로딩이 쿼리로 드러난다. 실제 DB를 쓰고, 끝나면 롤백된다.
 *
 * 코스 8601, 수업 8601(개념 8601). 모의고사 문항 8601~8610(10개, 모두 개념 8601), 보기는 문항마다 정답 1·오답 1.
 * 회원 8601~8605가 2020-01-01에 1회차를 제출했고 10문항 모두 오답을 골랐다 (응시자 5명 → 대시보드 집계 대상).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class ExamAnswerQueryCountTest {

    static final int QUESTIONS = 10;
    static final int RESPONDENTS = 5;
    static final long LESSON = 8601L;
    static final LocalDateTime FROM = LocalDateTime.of(2020, 1, 1, 0, 0);
    static final LocalDateTime TO = LocalDateTime.of(2020, 1, 2, 0, 0);

    @Autowired ExamReportService examReportService;
    @Autowired ExamReader examReader;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (8601, '쿼리 트랙', '대상', '설명', 8601)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (8601, '쿼리 분류', 8601)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (8601, 8601, 8601, '코스', '요약', '설명', '대상', '목표', 7)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) "
                + "VALUES (8601, 8601, '쿼리 수업', '소개', '목표', '내용', 10, 1, TRUE)");
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) "
                + "VALUES (8601, 8601, 'CONCEPT', '개념', '본문', 'MARKDOWN', 10, NULL, 1)");
        for (int q = 1; q <= QUESTIONS; q++) {
            long questionId = 8600L + q;
            jdbcTemplate.update("INSERT INTO exam_question (exam_question_id, lesson_id, concept_activity_id, content, explanation, sort_order, is_active) "
                    + "VALUES (?, 8601, 8601, ?, '해설', ?, TRUE)", questionId, "문항" + q, q);
            jdbcTemplate.update("INSERT INTO exam_choice (exam_choice_id, exam_question_id, content, is_correct, sort_order) VALUES "
                    + "(?, ?, '정답', TRUE, 1), (?, ?, '오답', FALSE, 2)", questionId * 10 + 1, questionId, questionId * 10 + 2, questionId);
        }
        for (int m = 1; m <= RESPONDENTS; m++) {
            long memberId = 8600L + m;
            jdbcTemplate.update("INSERT INTO member (member_id, email, nickname) VALUES (?, ?, ?)", memberId, "count-" + m + "@vibelog.test", "쿼리" + m);
            jdbcTemplate.update("INSERT INTO exam_attempt (exam_attempt_id, member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at) "
                    + "VALUES (?, ?, 8601, 1, '2020-01-01 10:00:00', 0, 0.00, '2020-01-01 11:00:00')", memberId, memberId);
            for (int q = 1; q <= QUESTIONS; q++) {
                long questionId = 8600L + q;
                jdbcTemplate.update("INSERT INTO exam_answer (exam_attempt_id, exam_question_id, exam_choice_id, is_correct) VALUES (?, ?, ?, FALSE)",
                        memberId, questionId, questionId * 10 + 2);
            }
        }
        jdbcTemplate.update("INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at) VALUES (8601, 8601, 8601, NOW())");
        jdbcTemplate.update("INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at) VALUES (8601, 8601, NOW())");
        entityManager.flush();
        entityManager.clear();
    }

    private Statistics clearedStatistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }

    @Test
    void 리포트는_문항이_10개여도_문항_보기를_답마다_조회하지_않는다() {
        Statistics statistics = clearedStatistics();

        ExamReportView report = examReportService.getReport(8601L, LESSON, 1).orElseThrow();

        // 결과는 그대로: 문항 순서대로 10개 모두 오답, 내가 고른 보기는 "오답"
        assertThat(report.totalCount()).isEqualTo(QUESTIONS);
        assertThat(report.wrongs()).extracting(ExamReportView.Wrong::number).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(report.wrongs()).extracting(ExamReportView.Wrong::myChoice).containsOnly("오답");
        // 답 목록 1번에 문항·보기가 함께 온다. 문항 수와 상관없이 늘지 않는다 (전: 문항 10개를 1번씩 = 지연 로딩 10번, SQL 24번)
        assertThat(statistics.getEntityFetchCount()).as("SQL %d번, 지연 로딩 %d번", statistics.getPrepareStatementCount(), statistics.getEntityFetchCount()).isZero();
    }

    @Test
    void 대시보드_약한_개념은_답_수만큼_문항을_조회하지_않는다() {
        Statistics statistics = clearedStatistics();

        List<ExamWeakConcept> weak = examReader.getWeakConceptsByLesson(FROM, TO).get(LESSON);

        // 결과는 그대로: 개념 8601, 50개 답 중 0개 맞힘
        assertThat(weak).singleElement().satisfies(concept -> {
            assertThat(concept.conceptActivityId()).isEqualTo(8601L);
            assertThat(concept.correctCount()).isZero();
            assertThat(concept.questionCount()).isEqualTo(QUESTIONS * RESPONDENTS);
        });
        // 답 목록 1번에 문항이 함께 온다 (전: 문항 10개를 1번씩 = 지연 로딩 10번, SQL 13번)
        assertThat(statistics.getEntityFetchCount()).as("SQL %d번, 지연 로딩 %d번", statistics.getPrepareStatementCount(), statistics.getEntityFetchCount()).isZero();
    }
}
