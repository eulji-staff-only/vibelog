package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dto.DropOffRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 활동별 이탈률(FR-ADM-003, #364)의 집계 규칙을 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다. 끝나면 모두 롤백된다.
 * 수업별 이탈률(DashboardAnalyticsReaderTest)과 같은 기준이다: 기간 안에 열린(unlocked_at) 진도만 세고, 그중 completed_at 이 없는 것이 "못 끝낸" 수.
 *
 * 테스트 코스 (9701), 수업 9701, 활동 3개, 수강 3명(E1~E3)
 *  - 활동 9711(개념): E1 1월 열림·완료, E2 1월 열림·미완료, E3 3월 열림·미완료(기간 밖)
 *  - 활동 9712(퀴즈): E1 1월 열림·미완료, E2 아직 안 열림(unlocked_at 없음, 제외)
 *  - 활동 9713: 진도 없음 (결과에 빠진다)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES "
                + "(9701, 'act-dropoff-1@vibelog.test', '이탈1'), (9702, 'act-dropoff-2@vibelog.test', '이탈2'), "
                + "(9703, 'act-dropoff-3@vibelog.test', '이탈3')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9701, '테스트 트랙', '대상', '설명', 9701)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9701, '테스트 카테고리', 9701)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9701, 9701, 9701, '코스 9701', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9701, 9701, '수업 1', '소개', '목표', '본문', 30, 1, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, sort_order, is_active) VALUES "
                + "(9711, 9701, 'CONCEPT', '개념 1', '본문', 'MARKDOWN', 10, 1, TRUE), "
                + "(9712, 9701, 'QUIZ', '퀴즈 1', NULL, NULL, 10, 2, TRUE), "
                + "(9713, 9701, 'CONCEPT', '개념 2', '본문', 'MARKDOWN', 10, 3, TRUE)",
        "INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at) VALUES "
                + "(9701, 9701, 9701, NOW()), (9702, 9702, 9701, NOW()), (9703, 9703, 9701, NOW())",
        "INSERT INTO activity_progress (enrollment_id, activity_id, unlocked_at, completed_at) VALUES "
                + "(9701, 9711, '2026-01-10 00:00:00', '2026-01-11 00:00:00'), "
                + "(9702, 9711, '2026-01-15 00:00:00', NULL), "
                + "(9703, 9711, '2026-03-01 00:00:00', NULL), "
                + "(9701, 9712, '2026-01-12 00:00:00', NULL), "
                + "(9702, 9712, NULL, NULL)"
})
class LessonReaderActivityDropOffTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

    @Autowired LessonReader lessonReader;

    @Test
    void 활동별_이탈률은_기간에_열린_진도만_세고_완료_못_한_수를_센다() {
        Map<Long, DropOffRate> rates = lessonReader.getDropOffRateByActivity(FROM, TO);

        assertThat(rates.get(9711L)).isEqualTo(new DropOffRate(2, 1));   // E1 완료, E2 미완료, E3(3월)은 기간 밖
        assertThat(rates.get(9712L)).isEqualTo(new DropOffRate(1, 1));   // E1 미완료, E2(안 열림)는 제외
    }

    @Test
    void 열린_진도가_없는_활동은_결과에_빠진다() {
        assertThat(lessonReader.getDropOffRateByActivity(FROM, TO)).doesNotContainKey(9713L);
    }

    @Test
    void 시작일_종료일이_null이면_그_쪽_제한이_없다() {
        assertThat(lessonReader.getDropOffRateByActivity(null, null).get(9711L)).isEqualTo(new DropOffRate(3, 2));
        assertThat(lessonReader.getDropOffRateByActivity(FROM, null).get(9711L)).isEqualTo(new DropOffRate(3, 2));
        assertThat(lessonReader.getDropOffRateByActivity(null, TO).get(9711L)).isEqualTo(new DropOffRate(2, 1));
    }

    @Test
    void 활동이_속한_수업_번호를_돌려주고_없는_활동은_빠진다() {
        assertThat(lessonReader.getLessonIdsByActivity(List.of(9711L, 9712L, 99999L)))
                .containsOnly(Map.entry(9711L, 9701L), Map.entry(9712L, 9701L));
    }
}
