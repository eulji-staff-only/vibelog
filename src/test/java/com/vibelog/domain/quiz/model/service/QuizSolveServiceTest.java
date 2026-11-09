package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.quiz.exception.QuizErrorCode;
import com.vibelog.domain.quiz.model.dto.QuizCheckResponse;
import com.vibelog.domain.quiz.model.dto.QuizPageView;
import com.vibelog.domain.quiz.model.dto.QuizStartResponse;
import com.vibelog.domain.quiz.model.dto.QuizSubmitResponse;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import com.vibelog.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 퀴즈 풀이·채점 (API-LSN-03~06, #108). 실제 DB 와 lesson 영역의 계약 ① 진짜 구현으로 본다.
 * 수업 9901: 퀴즈 9901(문항 2개, 통과 기준 50) → 개념 9902. 회원 A 는 수강 중이고 퀴즈가 열려 있다.
 * 문항 99011: 보기 990111(정답)·990112 / 문항 99012: 보기 990121(정답)·990122
 */
@SpringBootTest
@Transactional
class QuizSolveServiceTest {

    @Autowired
    QuizSolveService quizSolveService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    long memberA;
    long memberB;
    long enrollmentA;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9901, '퀴즈 트랙', '대상', '설명', 9901)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9901, '퀴즈 카테고리', 9901)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9901, 9901, 9901, '퀴즈 코스', '한 줄', '소개', '대상', '목표', 7, TRUE)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9901, 9901, '수업', '소개', '목표', '본문', 30, 1, TRUE)");
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9901, 9901, 'QUIZ', 'JSP 기초 퀴즈', NULL, NULL, 10, 50, 1), (9902, 9901, 'CONCEPT', '다음 개념', '본문', 'MARKDOWN', 10, NULL, 2)");
        jdbcTemplate.update("INSERT INTO concept_tag (concept_tag_id, name) VALUES (9901, 'JSP')");
        jdbcTemplate.update("INSERT INTO quiz_question (quiz_question_id, activity_id, concept_tag_id, display_type, content, code_snippet, explanation, sort_order, is_active) VALUES "
                + "(99011, 9901, 9901, 'CONCEPT', '문항1', NULL, '해설1', 1, TRUE), "
                + "(99012, 9901, 9901, 'OUTPUT_PREDICT', '문항2', '<%= 1 + 1 %>', '해설2', 2, TRUE), "
                + "(99013, 9901, 9901, 'CONCEPT', '사용 중지 문항', NULL, '해설3', 3, FALSE)");
        jdbcTemplate.update("INSERT INTO quiz_choice (quiz_choice_id, quiz_question_id, content, is_correct, sort_order) VALUES "
                + "(990111, 99011, '정답1', TRUE, 1), (990112, 99011, '오답1', FALSE, 2), "
                + "(990121, 99012, '2', TRUE, 1), (990122, 99012, '1 + 1', FALSE, 2), "
                + "(990131, 99013, '정답3', TRUE, 1)");

        memberA = member("quiz-a@vibelog.test");
        memberB = member("quiz-b@vibelog.test");
        jdbcTemplate.update("INSERT INTO enrollment (member_id, course_id, started_at) VALUES (?, 9901, NOW())", memberA);
        enrollmentA = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO activity_progress (enrollment_id, activity_id, unlocked_at) VALUES (?, 9901, NOW())", enrollmentA);
    }

    @Test
    void 퀴즈_화면은_제목과_사용_중인_문항_수를_보여주고_닫혀_있으면_빈_값() {
        QuizPageView page = quizSolveService.getPage(memberA, 9901L).orElseThrow();
        assertThat(page.title()).isEqualTo("JSP 기초 퀴즈");
        assertThat(page.questionCount()).isEqualTo(2);
        assertThat(page.bestCorrectRate()).isNull();
        assertThat(page.lessonId()).isEqualTo(9901L);

        assertThat(quizSolveService.getPage(memberB, 9901L)).isEmpty();   // 수강하지 않아 닫혀 있음
    }

    @Test
    void 처음_풀면_사용_중인_문항을_순서대로_주고_닫힌_퀴즈는_ACTIVITY_LOCKED() {
        QuizStartResponse start = quizSolveService.start(memberA, 9901L);

        assertThat(start.questions()).extracting(QuizStartResponse.Question::quizQuestionId).containsExactly(99011L, 99012L);
        assertThat(start.questions().get(1).displayType()).isEqualTo("OUTPUT_PREDICT");
        assertThat(start.questions().get(1).codeSnippet()).isEqualTo("<%= 1 + 1 %>");
        assertThat(start.questions().get(0).choices()).extracting(QuizStartResponse.Choice::quizChoiceId)
                .containsExactly(990111L, 990112L);

        assertError(() -> quizSolveService.start(memberB, 9901L), QuizErrorCode.ACTIVITY_LOCKED);
    }

    @Test
    void 다시_풀어도_같은_문항과_보기가_나온다() {
        quizSolveService.start(memberA, 9901L);
        QuizStartResponse retry = quizSolveService.start(memberA, 9901L);   // 순서는 섞일 수 있다 (PL-LSN-004·005)

        assertThat(retry.questions()).extracting(QuizStartResponse.Question::quizQuestionId).containsExactlyInAnyOrder(99011L, 99012L);
        assertThat(retry.questions()).flatExtracting(QuizStartResponse.Question::choices)
                .extracting(QuizStartResponse.Choice::quizChoiceId)
                .containsExactlyInAnyOrder(990111L, 990112L, 990121L, 990122L);
    }

    @Test
    void 확인하면_정답과_해설을_주고_같은_문항은_다시_확인할_수_없다() {
        Long attemptId = quizSolveService.start(memberA, 9901L).quizAttemptId();

        QuizCheckResponse wrong = quizSolveService.check(memberA, attemptId, 99011L, 990112L);
        assertThat(wrong.correct()).isFalse();
        assertThat(wrong.correctQuizChoiceId()).isEqualTo(990111L);
        assertThat(wrong.explanation()).isEqualTo("해설1");

        // 답 고정 (PL-LSN-006): 정답으로 바꾸려 해도 안 된다
        assertError(() -> quizSolveService.check(memberA, attemptId, 99011L, 990111L), QuizErrorCode.ALREADY_CHECKED);
    }

    @Test
    void 남의_풀이나_엉뚱한_보기는_확인할_수_없다() {
        Long attemptId = quizSolveService.start(memberA, 9901L).quizAttemptId();

        assertError(() -> quizSolveService.check(memberB, attemptId, 99011L, 990111L), CommonErrorCode.NOT_OWNER);
        assertError(() -> quizSolveService.check(memberA, 999999L, 99011L, 990111L), CommonErrorCode.NOT_OWNER);    // 없는 풀이
        assertError(() -> quizSolveService.check(memberA, attemptId, 99011L, 990121L), QuizErrorCode.INVALID_INPUT); // 다른 문항의 보기
        assertError(() -> quizSolveService.check(memberA, attemptId, 99013L, 990131L), QuizErrorCode.INVALID_INPUT); // 사용 중지 문항
    }

    @Test
    void 확인_안_한_문항이_있으면_제출할_수_없다() {
        Long attemptId = quizSolveService.start(memberA, 9901L).quizAttemptId();
        quizSolveService.check(memberA, attemptId, 99011L, 990111L);

        assertError(() -> quizSolveService.submit(memberA, attemptId), QuizErrorCode.QUIZ_NOT_FINISHED);
    }

    @Test
    void 통과하면_정답률을_기록하고_다음_개념을_연다() {
        Long attemptId = quizSolveService.start(memberA, 9901L).quizAttemptId();
        quizSolveService.check(memberA, attemptId, 99011L, 990111L);   // 정답
        quizSolveService.check(memberA, attemptId, 99012L, 990122L);   // 오답 → 1/2 = 50% (통과 기준과 같으면 통과)

        QuizSubmitResponse result = quizSolveService.submit(memberA, attemptId);
        entityManager.flush();   // JPA 변경 감지로 바뀐 값(완료·열림)을 DB 에 내보낸 뒤 JdbcTemplate 으로 읽는다

        assertThat(result.correctRate()).isEqualByComparingTo("50.00");
        assertThat(result.isPassed()).isTrue();
        assertThat(result.next().type()).isEqualTo("CONCEPT");
        assertThat(result.next().url()).isEqualTo("/courses/9901/lessons/9901?activity=9902");
        assertThat(result.message()).isEqualTo("다음 개념이 열렸습니다");

        Map<String, Object> attempt = jdbcTemplate.queryForMap("SELECT correct_rate, is_passed, submitted_at FROM quiz_attempt WHERE quiz_attempt_id = ?", attemptId);
        assertThat(attempt.get("is_passed")).isEqualTo(true);
        assertThat(attempt.get("submitted_at")).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT completed_at IS NOT NULL FROM activity_progress WHERE enrollment_id = ? AND activity_id = 9901", Boolean.class, enrollmentA)).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT unlocked_at IS NOT NULL FROM activity_progress WHERE enrollment_id = ? AND activity_id = 9902", Boolean.class, enrollmentA)).isTrue();

        // 제출한 풀이는 다시 제출·확인할 수 없고, 화면에는 최고 점수가 보인다
        assertError(() -> quizSolveService.submit(memberA, attemptId), QuizErrorCode.ATTEMPT_SUBMITTED);
        assertThat(quizSolveService.getPage(memberA, 9901L).map(QuizPageView::bestCorrectRate))
                .hasValueSatisfying(best -> assertThat(best).isEqualByComparingTo("50.00"));
    }

    @Test
    void 통과하지_못하면_다음_개념을_열지_않는다() {
        Long attemptId = quizSolveService.start(memberA, 9901L).quizAttemptId();
        quizSolveService.check(memberA, attemptId, 99011L, 990112L);   // 오답
        quizSolveService.check(memberA, attemptId, 99012L, 990122L);   // 오답 → 0%

        QuizSubmitResponse result = quizSolveService.submit(memberA, attemptId);
        entityManager.flush();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.next().type()).isEqualTo("NONE");
        assertThat(result.message()).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM activity_progress WHERE enrollment_id = ? AND activity_id = 9902", Integer.class, enrollmentA)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT completed_at IS NULL FROM activity_progress WHERE enrollment_id = ? AND activity_id = 9901", Boolean.class, enrollmentA)).isTrue();
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(expected);
    }

    private long member(String email) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', ?)", email, email.substring(0, 6));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
