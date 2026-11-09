package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.quiz.model.dto.WrongAnswerView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 틀린 문제 모아보기 (API-LSN-10, FR-LSN-008, #190)
 * - 내가 틀린 답만, 문항마다 가장 최근 답 하나, 최근 순. 나중에 맞혀도 남는다 (quiz.md 결정 1·2)
 * - 개념 링크: 문항의 개념 태그가 붙은 같은 수업의 사용 중인 개념 (결정 4). 없으면 수업 화면
 * - 코스·수업으로 거르고, 없는 조건이면 빈 목록
 * 수업 9871: 개념 98711(태그 9871, 사용 중) · 퀴즈 98712 · 개념 98713(태그 9872, 사용 중지)
 */
@SpringBootTest
@Transactional
class QuizWrongAnswerServiceTest {

    @Autowired
    QuizWrongAnswerService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    long memberA;
    long memberB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9871, '오답 트랙', '대상', '설명', 9871)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9871, '오답 카테고리', 9871)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9871, 9871, 9871, '오답 코스', '한 줄', '소개', '대상', '목표', 7, TRUE)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) "
                + "VALUES (9871, 9871, '오답 수업', '소개', '목표', '본문', 30, 1, TRUE)");
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order, is_active) VALUES "
                + "(98711, 9871, 'CONCEPT', '개념 A', '본문', 'MARKDOWN', 10, NULL, 1, TRUE), "
                + "(98712, 9871, 'QUIZ', '퀴즈', NULL, NULL, 10, 50, 2, TRUE), "
                + "(98713, 9871, 'CONCEPT', '개념 B', '본문', 'MARKDOWN', 10, NULL, 3, FALSE)");
        jdbcTemplate.update("INSERT INTO concept_tag (concept_tag_id, name) VALUES (9871, '오답 태그A'), (9872, '오답 태그B')");
        jdbcTemplate.update("INSERT INTO activity_tag (activity_id, concept_tag_id) VALUES (98711, 9871), (98713, 9872)");
        jdbcTemplate.update("INSERT INTO quiz_question (quiz_question_id, activity_id, concept_tag_id, display_type, content, code_snippet, explanation, sort_order) VALUES "
                + "(98721, 98712, 9871, 'OUTPUT_PREDICT', '문항 가', 'print(1)', '해설 가', 1), "
                + "(98722, 98712, 9872, 'CONCEPT', '문항 나', NULL, '해설 나', 2)");
        jdbcTemplate.update("INSERT INTO quiz_choice (quiz_choice_id, quiz_question_id, content, is_correct, sort_order) VALUES "
                + "(987211, 98721, '1', TRUE, 1), (987212, 98721, '2', FALSE, 2), (987213, 98721, '3', FALSE, 3), "
                + "(987221, 98722, '맞는 답', TRUE, 1), (987222, 98722, '틀린 답', FALSE, 2)");

        memberA = member("wrong-a@vibelog.test");
        memberB = member("wrong-b@vibelog.test");
        long first = attempt(memberA);
        long second = attempt(memberA);
        answer(first, 98721, 987212, false, "2026-11-01 10:00:00");
        answer(first, 98722, 987222, false, "2026-11-01 10:01:00");
        answer(second, 98721, 987213, false, "2026-11-02 10:00:00");   // 가: 다시 틀림 (최근 답 3)
        answer(second, 98722, 987221, true, "2026-11-02 10:01:00");    // 나: 나중에 맞힘 → 그래도 남는다
        answer(attempt(memberB), 98721, 987212, false, "2026-11-03 10:00:00");   // 다른 회원
    }

    @Test
    void 틀린_문항을_문항마다_최근_답_하나로_최근_순으로_모으고_다시_맞혀도_남긴다() {
        WrongAnswerView view = service.getPage(memberA, null, null, 1);

        assertThat(view.items()).extracting(WrongAnswerView.Item::content).containsExactly("문항 가", "문항 나");
        WrongAnswerView.Item ga = view.items().get(0);
        assertThat(ga.myAnswer()).isEqualTo("3");
        assertThat(ga.correctAnswer()).isEqualTo("1");
        assertThat(ga.codeSnippet()).isEqualTo("print(1)");
        assertThat(ga.displayTypeLabel()).isEqualTo("결과 유추하기");
        assertThat(ga.courseTitle()).isEqualTo("오답 코스");
        assertThat(ga.lessonTitle()).isEqualTo("오답 수업");
        assertThat(ga.conceptTagName()).isEqualTo("오답 태그A");
        assertThat(view.totalCount()).isEqualTo(2);
    }

    @Test
    void 개념_링크는_태그가_붙은_사용_중인_개념이고_없으면_수업_화면이다() {
        WrongAnswerView view = service.getPage(memberA, null, null, 1);

        assertThat(view.items().get(0).conceptUrl()).isEqualTo("/courses/9871/lessons/9871?activity=98711");
        // 태그 B 는 사용 중지한 개념에만 붙어 있다 → 수업 화면
        assertThat(view.items().get(1).conceptUrl()).isEqualTo("/courses/9871/lessons/9871");
    }

    @Test
    void 코스_수업으로_거르고_필터_칸은_내가_틀린_곳만_보여준다() {
        WrongAnswerView view = service.getPage(memberA, 9871L, 9871L, 1);
        assertThat(view.totalCount()).isEqualTo(2);
        assertThat(view.courseOptions()).extracting(WrongAnswerView.Option::title).containsExactly("오답 코스");
        assertThat(view.lessonOptions()).extracting(WrongAnswerView.Option::title).containsExactly("오답 수업");

        assertThat(service.getPage(memberA, 9879L, null, 1).isEmpty()).isTrue();
        assertThat(service.getPage(memberA, null, 9879L, 1).isEmpty()).isTrue();
    }

    @Test
    void 운영자가_사용_중지한_문항도_그대로_보여준다() {
        jdbcTemplate.update("UPDATE quiz_question SET is_active = FALSE WHERE quiz_question_id = 98721");

        assertThat(service.getPage(memberA, null, null, 1).items()).extracting(WrongAnswerView.Item::content).contains("문항 가");
    }

    @Test
    void 틀린_문제가_없으면_빈_목록이고_쪽은_1부터다() {
        long none = member("wrong-none@vibelog.test");
        WrongAnswerView view = service.getPage(none, null, null, 0);

        assertThat(view.isEmpty()).isTrue();
        assertThat(view.page()).isEqualTo(1);
        assertThat(view.totalPages()).isEqualTo(1);
    }

    private long member(String email) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', ?)", email, email.substring(0, 7));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long attempt(long memberId) {
        jdbcTemplate.update("INSERT INTO quiz_attempt (member_id, activity_id, started_at) VALUES (?, 98712, NOW())", memberId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void answer(long attemptId, long questionId, long choiceId, boolean correct, String checkedAt) {
        jdbcTemplate.update("INSERT INTO quiz_answer (quiz_attempt_id, quiz_question_id, quiz_choice_id, is_correct, checked_at) VALUES (?, ?, ?, ?, ?)",
                attemptId, questionId, choiceId, correct, checkedAt);
    }
}
