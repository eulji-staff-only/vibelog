package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.quiz.model.policy.LessonQuizScoreChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/* 계약 ⑤ LessonQuizScoreChecker (#84): 제출한 풀이 중 가장 높은 정답률, 없으면 null (PL-LSN-007) */
@SpringBootTest
@Transactional
class QuizContractServiceTest {

    @Autowired
    LessonQuizScoreChecker lessonQuizScoreChecker;

    @Autowired
    JdbcTemplate jdbcTemplate;

    long memberId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9601, '트랙', '대상', '설명', 9601)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9601, '카테고리', 9601)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9601, 9601, 9601, '코스', '요약', '설명', '대상', '목표', 7, TRUE)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9601, 9601, '수업', '소개', '목표', '본문', 30, 1, TRUE)");
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9601, 9601, 'QUIZ', '퀴즈1', NULL, NULL, 10, 50, 1), (9602, 9601, 'QUIZ', '퀴즈2', NULL, NULL, 10, 50, 2)");
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES ('quiz-score@vibelog.test', 'x', '퀴즈회원')");
        memberId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void 제출한_풀이가_없으면_null() {
        // 제출 전 풀이(정답률 없음)만 있는 경우도 "없음"으로 본다
        jdbcTemplate.update("INSERT INTO quiz_attempt (member_id, activity_id, started_at) VALUES (?, 9601, NOW())", memberId);

        assertThat(lessonQuizScoreChecker.getBestCorrectRate(memberId, 9601L)).isNull();
    }

    @Test
    void 여러_번_제출하면_가장_높은_정답률() {
        submitted(9601L, "40.00", false);
        submitted(9601L, "80.00", true);
        submitted(9601L, "60.00", true);
        submitted(9602L, "100.00", true);   // 다른 퀴즈 기록은 섞이지 않는다

        assertThat(lessonQuizScoreChecker.getBestCorrectRate(memberId, 9601L)).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    private void submitted(Long activityId, String correctRate, boolean passed) {
        jdbcTemplate.update("INSERT INTO quiz_attempt (member_id, activity_id, started_at, correct_rate, is_passed, submitted_at) "
                + "VALUES (?, ?, NOW(), ?, ?, NOW())", memberId, activityId, new BigDecimal(correctRate), passed);
    }
}
