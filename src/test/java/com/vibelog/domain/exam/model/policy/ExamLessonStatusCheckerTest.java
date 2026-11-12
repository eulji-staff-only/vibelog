package com.vibelog.domain.exam.model.policy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 계약 ④ getLatestSubmittedAttemptNos 가 수업별 마지막 "제출" 회차만 돌려주는지 확인한다.
 * 테스트 데이터는 테스트 트랜잭션 안에서 넣고, 끝나면 롤백된다 (ID 는 샘플 데이터와 겹치지 않게 9000번대).
 *
 * 회원 9001
 *   수업 9001: 1회차 제출, 2회차 제출, 3회차 제출 안 함 → 2
 *   수업 9002: 1회차 제출 안 함                       → 빠짐
 *   수업 9003: 응시 없음                               → 빠짐
 * 회원 9002
 *   수업 9002: 1회차 제출 (다른 회원 기록이라 회원 9001 결과에 섞이면 안 됨)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES (9001, 'exam-a@vibelog.test', '응시자A'), (9002, 'exam-b@vibelog.test', '응시자B')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9001, '테스트트랙', '대상', '설명', 9001)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9001, '테스트분류', 9001)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES (9001, 9001, 9001, '코스', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order) VALUES "
                + "(9001, 9001, '수업1', '소개', '목표', '내용', 10, 1), (9002, 9001, '수업2', '소개', '목표', '내용', 10, 2), (9003, 9001, '수업3', '소개', '목표', '내용', 10, 3)",
        "INSERT INTO exam_attempt (member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at) VALUES "
                + "(9001, 9001, 1, NOW(), 3, 60.00, NOW()), "
                + "(9001, 9001, 2, NOW(), 5, 100.00, NOW()), "
                + "(9001, 9001, 3, NOW(), NULL, NULL, NULL), "
                + "(9001, 9002, 1, NOW(), NULL, NULL, NULL), "
                + "(9002, 9002, 1, NOW(), 4, 80.00, NOW())"
})
class ExamLessonStatusCheckerTest {

    @Autowired
    LessonExamStatusChecker checker;

    @Test
    void 수업별_마지막_제출_회차만_돌려주고_제출없는_수업은_빠진다() {
        Map<Long, Integer> result = checker.getLatestSubmittedAttemptNos(9001L, List.of(9001L, 9002L, 9003L));

        assertThat(result).containsExactly(Map.entry(9001L, 2));
    }

    @Test
    void 수업_목록이_비면_빈_결과() {
        assertThat(checker.getLatestSubmittedAttemptNos(9001L, List.of())).isEmpty();
    }
}
