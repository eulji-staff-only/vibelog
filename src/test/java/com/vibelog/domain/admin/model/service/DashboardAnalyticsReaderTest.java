package com.vibelog.domain.admin.model.service;

import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.exam.model.dto.ExamWeakConcept;
import com.vibelog.domain.exam.model.service.ExamReader;
import com.vibelog.domain.lesson.model.dto.DropOffRate;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.quiz.model.dto.ConceptWrongStat;
import com.vibelog.domain.quiz.model.dto.QuizPassRate;
import com.vibelog.domain.quiz.model.service.QuizReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 확장 대시보드(API-ADM-40, FR-ADM-003, #194 2차)가 쓰는 각 영역 Reader 의 기간별 집계. 테스트가 끝나면 롤백된다. ID 는 9510~9549 만 쓴다.
 * 기간 P = 2020-05-01 00:00 ~ 2020-05-31 23:59:59 (시드 데이터는 모두 이후 날짜라 겹치지 않는다).
 *
 * 추천 트랙(진단): 회원 9510 은 5/1 트랙9510 → 5/20 트랙9511 (가장 최근 완료), 9511·9512 는 트랙9510 (9512 는 5/25 건너뜀이 더 최근이어도 완료만 본다),
 *   9513 은 6월(기간 밖), 9514 는 건너뜀만. 수강: 9511 은 코스9510·9511, 나머지는 코스9510.
 * 이탈률: 수업9510 은 기간 안 열림 3(완료 1, 못 끝냄 2) + 안 열림 1(제외) + 6월 열림 1, 수업9511 은 기간 안 1(완료) + 4월 말 열림 1(기간 밖, 미완료)
 * 퀴즈 통과율: 기간 안 (회원, 퀴즈) 4쌍 = 9510·퀴즈9512(1차 실패→2차 통과), 9511·퀴즈9512(실패), 9512·퀴즈9512(통과 후 나중에 0점), 9510·퀴즈9513(75점, 기준 80%라 실패)
 *   → 응시 4 · 통과 2. 제출 안 한 풀이와 6월 풀이는 제외.
 * 개념별 오답: 태그9510 은 12번 답해 7번 틀림(약한 개념), 태그9511 은 5번 답해 2번 틀림(정답률 정확히 60%, 약하지 않음)
 * 모의고사: 수업9510 은 1회차 제출 5명(100·80·60·40·21 → 평균 60.2) + 테스트 계정·운영자·2회차·6월 응시(모두 0점, 제외 대상),
 *   수업9511 은 1회차 4명(5명 미만이라 숨김). 수업9510 약한 개념: 개념9511(2/5), 개념9510 은 6/10 = 정확히 60% 라 약하지 않음.
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname, role, is_test) VALUES "
                + "(9510, 'an-r1@vibelog.test', '분석1', 'LEARNER', FALSE), (9511, 'an-r2@vibelog.test', '분석2', 'LEARNER', FALSE), "
                + "(9512, 'an-r3@vibelog.test', '분석3', 'LEARNER', FALSE), (9513, 'an-r4@vibelog.test', '분석4', 'LEARNER', FALSE), "
                + "(9514, 'an-r5@vibelog.test', '분석5', 'LEARNER', FALSE), (9515, 'an-r6@vibelog.test', '분석테스트', 'LEARNER', TRUE), "
                + "(9516, 'an-r7@vibelog.test', '분석운영', 'ADMIN', FALSE), (9517, 'an-r8@vibelog.test', '분석8', 'LEARNER', FALSE), "
                + "(9518, 'an-r9@vibelog.test', '분석9', 'LEARNER', FALSE), (9519, 'an-r10@vibelog.test', '분석10', 'LEARNER', FALSE), "
                + "(9520, 'an-r11@vibelog.test', '분석11', 'LEARNER', FALSE), (9521, 'an-r12@vibelog.test', '분석12', 'LEARNER', FALSE)",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES "
                + "(9510, '분석트랙A', '대상', '설명', 9510), (9511, '분석트랙B', '대상', '설명', 9511)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9510, '분석분류', 9510)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES "
                + "(9510, 9510, 9510, '분석코스A', '요약', '설명', '대상', '목표', 7), (9511, 9511, 9510, '분석코스B', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9510, 9510, '분석수업1', '소개', '목표', '내용', 10, 1, TRUE), (9511, 9510, '분석수업2', '소개', '목표', '내용', 10, 2, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, sort_order, pass_rate) VALUES "
                + "(9510, 9510, 'CONCEPT', '분석개념1', 1, NULL), (9511, 9510, 'CONCEPT', '분석개념2', 2, NULL), "
                + "(9512, 9510, 'QUIZ', '분석퀴즈1', 3, 50), (9513, 9511, 'QUIZ', '분석퀴즈2', 1, 80)",
        "INSERT INTO concept_tag (concept_tag_id, name) VALUES (9510, '분석태그A'), (9511, '분석태그B')",
        // 진단과 수강
        "INSERT INTO diagnosis_result (member_id, recommended_track_id, status, created_at) VALUES "
                + "(9510, 9510, 'COMPLETED', '2020-05-01 00:00:00'), (9510, 9511, 'COMPLETED', '2020-05-20 10:00:00'), "
                + "(9511, 9510, 'COMPLETED', '2020-05-10 10:00:00'), "
                + "(9512, 9510, 'COMPLETED', '2020-05-12 10:00:00'), (9512, NULL, 'SKIPPED', '2020-05-25 10:00:00'), "
                + "(9513, 9510, 'COMPLETED', '2020-06-03 10:00:00'), (9514, NULL, 'SKIPPED', '2020-05-05 10:00:00')",
        "INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at) VALUES "
                + "(9510, 9510, 9510, '2020-05-02 00:00:00'), (9511, 9511, 9510, '2020-05-02 00:00:00'), (9512, 9512, 9510, '2020-05-02 00:00:00'), "
                + "(9513, 9513, 9510, '2020-05-02 00:00:00'), (9514, 9514, 9510, '2020-05-02 00:00:00'), (9515, 9511, 9511, '2020-05-03 00:00:00')",
        // 수업 진도 (이탈률)
        "INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at, completed_at) VALUES "
                + "(9510, 9510, '2020-05-02 10:00:00', '2020-05-03 10:00:00'), (9511, 9510, '2020-05-02 10:00:00', NULL), "
                + "(9512, 9510, '2020-05-31 23:00:00', NULL), (9513, 9510, NULL, NULL), (9514, 9510, '2020-06-01 00:00:00', NULL), "
                + "(9510, 9511, '2020-05-04 10:00:00', '2020-05-05 10:00:00'), (9511, 9511, '2020-04-30 23:00:00', NULL)",
        // 퀴즈
        "INSERT INTO quiz_question (quiz_question_id, activity_id, concept_tag_id, display_type, content, explanation, sort_order) VALUES "
                + "(9510, 9512, 9510, 'CONCEPT', '문항1', '해설', 1), (9511, 9512, 9510, 'CONCEPT', '문항2', '해설', 2), "
                + "(9512, 9512, 9511, 'CONCEPT', '문항3', '해설', 3)",
        "INSERT INTO quiz_choice (quiz_choice_id, quiz_question_id, content, is_correct, sort_order) VALUES "
                + "(9510, 9510, '보기', TRUE, 1), (9511, 9511, '보기', TRUE, 1), (9512, 9512, '보기', TRUE, 1)",
        "INSERT INTO quiz_attempt (quiz_attempt_id, member_id, activity_id, started_at, correct_rate, is_passed, submitted_at) VALUES "
                + "(9510, 9510, 9512, '2020-05-05 09:00:00', 33.33, FALSE, '2020-05-05 10:00:00'), "
                + "(9511, 9510, 9512, '2020-05-06 09:00:00', 100.00, TRUE, '2020-05-06 10:00:00'), "
                + "(9512, 9511, 9512, '2020-05-07 09:00:00', 33.33, FALSE, '2020-05-07 10:00:00'), "
                + "(9513, 9511, 9512, '2020-05-08 09:00:00', 0.00, FALSE, '2020-05-08 10:00:00'), "
                + "(9514, 9512, 9512, '2020-05-09 09:00:00', 100.00, TRUE, '2020-05-09 10:00:00'), "
                + "(9515, 9512, 9512, '2020-05-10 09:00:00', 0.00, FALSE, '2020-05-10 10:00:00'), "
                + "(9516, 9510, 9513, '2020-05-11 09:00:00', 75.00, FALSE, '2020-05-11 10:00:00'), "
                + "(9517, 9513, 9512, '2020-05-12 09:00:00', NULL, NULL, NULL), "
                + "(9518, 9513, 9512, '2020-06-02 09:00:00', 100.00, TRUE, '2020-06-02 10:00:00')",
        "INSERT INTO quiz_answer (quiz_attempt_id, quiz_question_id, quiz_choice_id, is_correct, checked_at) VALUES "
                + "(9510, 9510, 9510, FALSE, '2020-05-05 10:00:00'), (9510, 9511, 9511, FALSE, '2020-05-05 10:00:00'), (9510, 9512, 9512, TRUE, '2020-05-05 10:00:00'), "
                + "(9511, 9510, 9510, TRUE, '2020-05-06 10:00:00'), (9511, 9511, 9511, TRUE, '2020-05-06 10:00:00'), (9511, 9512, 9512, TRUE, '2020-05-06 10:00:00'), "
                + "(9512, 9510, 9510, FALSE, '2020-05-07 10:00:00'), (9512, 9511, 9511, TRUE, '2020-05-07 10:00:00'), (9512, 9512, 9512, FALSE, '2020-05-07 10:00:00'), "
                + "(9513, 9510, 9510, FALSE, '2020-05-08 10:00:00'), (9513, 9511, 9511, FALSE, '2020-05-08 10:00:00'), "
                + "(9514, 9510, 9510, TRUE, '2020-05-09 10:00:00'), (9514, 9511, 9511, TRUE, '2020-05-09 10:00:00'), (9514, 9512, 9512, TRUE, '2020-05-09 10:00:00'), "
                + "(9515, 9510, 9510, FALSE, '2020-05-10 10:00:00'), (9515, 9511, 9511, FALSE, '2020-05-10 10:00:00'), (9515, 9512, 9512, FALSE, '2020-05-10 10:00:00'), "
                + "(9517, 9510, 9510, FALSE, '2020-05-12 10:00:00'), "
                + "(9518, 9510, 9510, FALSE, '2020-06-02 10:00:00')",
        // 모의고사
        "INSERT INTO exam_question (exam_question_id, lesson_id, concept_activity_id, content, explanation, sort_order) VALUES "
                + "(9510, 9510, 9510, '문항1', '해설', 1), (9511, 9510, 9510, '문항2', '해설', 2), (9512, 9510, 9511, '문항3', '해설', 3)",
        "INSERT INTO exam_attempt (exam_attempt_id, member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at) VALUES "
                + "(9510, 9510, 9510, 1, '2020-05-05 09:00:00', 100, 100.00, '2020-05-05 10:00:00'), "
                + "(9511, 9511, 9510, 1, '2020-05-05 09:00:00', 80, 80.00, '2020-05-05 10:00:00'), "
                + "(9512, 9512, 9510, 1, '2020-05-05 09:00:00', 60, 60.00, '2020-05-05 10:00:00'), "
                + "(9513, 9513, 9510, 1, '2020-05-05 09:00:00', 40, 40.00, '2020-05-05 10:00:00'), "
                + "(9514, 9514, 9510, 1, '2020-05-05 09:00:00', 21, 21.00, '2020-05-05 10:00:00'), "
                + "(9515, 9515, 9510, 1, '2020-05-05 09:00:00', 0, 0.00, '2020-05-05 10:00:00'), "
                + "(9516, 9516, 9510, 1, '2020-05-05 09:00:00', 0, 0.00, '2020-05-05 10:00:00'), "
                + "(9517, 9510, 9510, 2, '2020-05-20 09:00:00', 0, 0.00, '2020-05-20 10:00:00'), "
                + "(9518, 9521, 9510, 1, '2020-06-05 09:00:00', 0, 0.00, '2020-06-05 10:00:00'), "
                + "(9519, 9517, 9511, 1, '2020-05-06 09:00:00', 50, 50.00, '2020-05-06 10:00:00'), "
                + "(9520, 9518, 9511, 1, '2020-05-06 09:00:00', 50, 50.00, '2020-05-06 10:00:00'), "
                + "(9521, 9519, 9511, 1, '2020-05-06 09:00:00', 50, 50.00, '2020-05-06 10:00:00'), "
                + "(9522, 9520, 9511, 1, '2020-05-06 09:00:00', 50, 50.00, '2020-05-06 10:00:00')",
        // 개념9510 문항(9510·9511) 6/10 정답, 개념9511 문항(9512) 2/5 정답. 제외 대상 응시(9515~9518)는 모두 오답
        "INSERT INTO exam_answer (exam_attempt_id, exam_question_id, exam_choice_id, is_correct) VALUES "
                + "(9510, 9510, NULL, TRUE), (9510, 9511, NULL, TRUE), (9510, 9512, NULL, TRUE), "
                + "(9511, 9510, NULL, TRUE), (9511, 9511, NULL, TRUE), (9511, 9512, NULL, TRUE), "
                + "(9512, 9510, NULL, TRUE), (9512, 9511, NULL, FALSE), (9512, 9512, NULL, FALSE), "
                + "(9513, 9510, NULL, TRUE), (9513, 9511, NULL, FALSE), (9513, 9512, NULL, FALSE), "
                + "(9514, 9510, NULL, FALSE), (9514, 9511, NULL, FALSE), (9514, 9512, NULL, NULL), "
                + "(9515, 9510, NULL, FALSE), (9515, 9511, NULL, FALSE), (9515, 9512, NULL, FALSE), "
                + "(9516, 9510, NULL, FALSE), (9516, 9511, NULL, FALSE), (9516, 9512, NULL, FALSE), "
                + "(9517, 9510, NULL, FALSE), (9517, 9511, NULL, FALSE), (9517, 9512, NULL, FALSE), "
                + "(9518, 9510, NULL, FALSE), (9518, 9511, NULL, FALSE), (9518, 9512, NULL, FALSE)"
})
class DashboardAnalyticsReaderTest {

    static final LocalDateTime FROM = LocalDateTime.of(2020, 5, 1, 0, 0);
    static final LocalDateTime TO = LocalDateTime.of(2020, 5, 31, 23, 59, 59);

    @Autowired CourseReader courseReader;
    @Autowired LessonReader lessonReader;
    @Autowired QuizReader quizReader;
    @Autowired ExamReader examReader;

    // ---- 추천 트랙 ----

    @Test
    void 추천_트랙은_기간_안_가장_최근_완료_진단의_트랙이고_건너뜀과_기간_밖은_뺀다() {
        Map<Long, Long> recommended = courseReader.findDiagnosisRecommendedTracks(FROM, TO);

        assertThat(recommended).containsEntry(9510L, 9511L)   // 5/1 트랙9510 이 아니라 5/20 트랙9511
                .containsEntry(9511L, 9510L)
                .containsEntry(9512L, 9510L)                   // 더 최근 건너뜀 기록이 덮어쓰지 않는다
                .doesNotContainKeys(9513L, 9514L);             // 6월 완료(기간 밖), 건너뜀만 있는 회원
    }

    @Test
    void 추천_트랙은_시작일_종료일이_null이면_그_쪽_제한이_없다() {
        assertThat(courseReader.findDiagnosisRecommendedTracks(null, TO)).containsEntry(9510L, 9511L).doesNotContainKey(9513L);
        assertThat(courseReader.findDiagnosisRecommendedTracks(FROM, null)).containsEntry(9513L, 9510L).containsEntry(9510L, 9511L);
        assertThat(courseReader.findDiagnosisRecommendedTracks(null, null)).containsEntry(9513L, 9510L);
    }

    @Test
    void 추천_트랙은_기간_안의_진단만_보므로_더_최근_진단이_기간_밖이면_그_앞_진단을_쓴다() {
        // 5/15 까지만 보면 회원 9510 의 5/20 진단은 기간 밖이라 5/1 진단(트랙9510)이 남는다
        Map<Long, Long> recommended = courseReader.findDiagnosisRecommendedTracks(FROM, LocalDateTime.of(2020, 5, 15, 0, 0));

        assertThat(recommended).containsEntry(9510L, 9510L).containsEntry(9511L, 9510L);
    }

    @Test
    void 수강한_코스는_회원별로_모으고_수강이_없는_회원은_빠진다() {
        Map<Long, Set<Long>> enrolled = lessonReader.findEnrolledCourseIds(List.of(9511L, 9510L, 9515L));

        assertThat(enrolled).containsEntry(9511L, Set.of(9510L, 9511L)).containsEntry(9510L, Set.of(9510L)).doesNotContainKey(9515L);
        assertThat(lessonReader.findEnrolledCourseIds(List.of())).isEmpty();
    }

    // ---- 이탈률 ----

    @Test
    void 수업별_이탈률은_기간에_열린_진도만_세고_완료_못_한_수를_센다() {
        Map<Long, DropOffRate> rates = lessonReader.getDropOffRateByLesson(FROM, TO);

        assertThat(rates.get(9510L)).isEqualTo(new DropOffRate(3, 2));   // 안 열림(unlocked_at 없음)·6월 열림은 제외
        assertThat(rates.get(9511L)).isEqualTo(new DropOffRate(1, 0));   // 4월 말 열림은 기간 밖
    }

    @Test
    void 수업별_이탈률은_시작일_종료일이_null이면_그_쪽_제한이_없다() {
        assertThat(lessonReader.getDropOffRateByLesson(null, TO).get(9511L)).isEqualTo(new DropOffRate(2, 1));
        assertThat(lessonReader.getDropOffRateByLesson(FROM, null).get(9510L)).isEqualTo(new DropOffRate(4, 3));
        assertThat(lessonReader.getDropOffRateByLesson(null, null).get(9510L)).isEqualTo(new DropOffRate(4, 3));
    }

    @Test
    void 수업별_이탈률은_열린_진도가_없는_기간이면_그_수업이_빠진다() {
        Map<Long, DropOffRate> rates = lessonReader.getDropOffRateByLesson(LocalDateTime.of(2019, 1, 1, 0, 0), LocalDateTime.of(2019, 1, 31, 23, 59));

        assertThat(rates).doesNotContainKeys(9510L, 9511L);
    }

    @Test
    void 수업과_코스를_이어_주는_값을_읽는다() {
        assertThat(lessonReader.getCourseIdsByLesson(List.of(9510L, 9511L))).containsEntry(9510L, 9510L).containsEntry(9511L, 9510L);
    }

    // ---- 퀴즈 ----

    @Test
    void 퀴즈_통과율은_회원과_퀴즈마다_최고_점수_풀이_하나로_센다() {
        QuizPassRate rate = quizReader.getPassRate(FROM, TO);

        // 9510·퀴즈9512(실패 뒤 통과) 통과, 9511 실패, 9512(통과 뒤 0점) 통과, 9510·퀴즈9513(75점, 기준 80) 실패
        assertThat(rate).isEqualTo(new QuizPassRate(4, 2));
    }

    @Test
    void 퀴즈_통과율은_시작일_종료일이_null이면_그_쪽_제한이_없다() {
        assertThat(quizReader.getPassRate(null, TO)).isEqualTo(new QuizPassRate(4, 2));
        QuizPassRate fromOnly = quizReader.getPassRate(FROM, null);   // 6월 통과 풀이 한 쌍이 더 들어온다
        assertThat(fromOnly.attemptedCount()).isGreaterThanOrEqualTo(5);
        assertThat(fromOnly.passedCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void 퀴즈_통과율은_풀이가_없는_기간이면_0이다() {
        assertThat(quizReader.getPassRate(LocalDateTime.of(2019, 1, 1, 0, 0), LocalDateTime.of(2019, 1, 31, 23, 59)))
                .isEqualTo(new QuizPassRate(0, 0));
    }

    @Test
    void 개념별_오답은_제출한_풀이의_문항별_정오답을_기간_안에서만_센다() {
        Map<Long, ConceptWrongStat> stats = quizReader.getWrongRateByConceptTag(FROM, TO);

        // 제출 안 한 풀이(9517)와 6월 풀이(9518)의 오답은 세지 않는다
        assertThat(stats.get(9510L)).isEqualTo(new ConceptWrongStat(12, 7));
        assertThat(stats.get(9511L)).isEqualTo(new ConceptWrongStat(5, 2));
    }

    @Test
    void 개념별_오답은_시작일_종료일이_null이면_그_쪽_제한이_없다() {
        assertThat(quizReader.getWrongRateByConceptTag(null, TO).get(9510L)).isEqualTo(new ConceptWrongStat(12, 7));
        ConceptWrongStat fromOnly = quizReader.getWrongRateByConceptTag(FROM, null).get(9510L);   // 6월 오답 1건이 더 들어온다
        assertThat(fromOnly.questionCount()).isGreaterThanOrEqualTo(13);
        assertThat(fromOnly.wrongCount()).isGreaterThanOrEqualTo(8);
    }

    // ---- 모의고사 ----

    @Test
    void 모의고사_평균은_첫_응시만_운영자와_테스트_계정을_빼고_소수_첫째_자리로_낸다() {
        Map<Long, Double> averages = examReader.getAverageScoreByLesson(FROM, TO);

        // (100+80+60+40+21) / 5 = 60.2. 테스트 계정·운영자·2회차·6월 응시(0점)는 들어가지 않는다
        assertThat(averages).containsEntry(9510L, 60.2);
    }

    @Test
    void 모의고사_응시자가_5명_미만인_수업은_평균과_약한_개념에서_숨긴다() {
        assertThat(examReader.getAverageScoreByLesson(FROM, TO)).doesNotContainKey(9511L);   // 4명
        assertThat(examReader.getWeakConceptsByLesson(FROM, TO)).doesNotContainKey(9511L);
    }

    @Test
    void 모의고사_평균은_시작일_종료일이_null이면_그_쪽_제한이_없다() {
        assertThat(examReader.getAverageScoreByLesson(null, TO)).containsEntry(9510L, 60.2);
        // 6월에 0점 응시가 한 명 더 들어오면 6명 (301 / 6 = 50.2)
        assertThat(examReader.getAverageScoreByLesson(FROM, null)).containsEntry(9510L, 50.2);
        assertThat(examReader.getAverageScoreByLesson(null, null)).containsEntry(9510L, 50.2);
    }

    @Test
    void 모의고사_약한_개념은_정답률_60퍼센트_미만만_담고_정확히_60퍼센트는_뺀다() {
        List<ExamWeakConcept> weak = examReader.getWeakConceptsByLesson(FROM, TO).get(9510L);

        // 개념9510: 6/10 = 60% → 약하지 않음, 개념9511: 2/5 = 40% → 약함. 제외 대상 응시의 오답은 세지 않는다
        assertThat(weak).containsExactly(new ExamWeakConcept(9511L, 2, 5));
    }

    @Test
    void 모의고사_약한_개념은_기간_밖_응시를_세지_않는다() {
        // 6월 응시(오답 3개)가 들어오면 개념9510 이 6/12 로 약해진다
        List<ExamWeakConcept> includingJune = examReader.getWeakConceptsByLesson(FROM, null).get(9510L);

        assertThat(includingJune).extracting(ExamWeakConcept::conceptActivityId).contains(9510L, 9511L);
        assertThat(examReader.getWeakConceptsByLesson(FROM, TO).get(9510L)).extracting(ExamWeakConcept::conceptActivityId)
                .doesNotContain(9510L);
    }
}
