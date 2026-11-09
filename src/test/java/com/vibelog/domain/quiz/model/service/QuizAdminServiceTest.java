package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.quiz.exception.QuizErrorCode;
import com.vibelog.domain.quiz.model.dto.AdminQuizQuestionItem;
import com.vibelog.domain.quiz.model.dto.QuizQuestionForm;
import com.vibelog.domain.quiz.model.entity.QuizDisplayType;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 퀴즈 문항 등록 (API-ADM-26~28, FR-LSN-012, #102)
 * - 보기 4개·정답 1개가 아니거나, 코드 빈칸 채우기·결과 유추하기에 코드가 없으면 저장되지 않는다
 * - 풀이 기록이 있는 문항을 고치면 기존 문항은 중지되고 새 문항으로 저장된다. 기존 학습자의 답은 그대로다 (DB 설계서 5-4)
 * 활동 9771(퀴즈)·9772(개념), 개념 태그 9771·9772, 문항 97711(풀이 기록 없음)·97712(풀이 기록 있음)
 */
@SpringBootTest
@Transactional
class QuizAdminServiceTest {

    @Autowired
    QuizAdminService quizAdminService;

    @Autowired
    QuizReader quizReader;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9771, '문항관리 트랙', '대상', '설명', 9771)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9771, '문항관리 카테고리', 9771)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9771, 9771, 9771, '문항관리 코스', '한 줄', '소개', '대상', '목표', 7)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order) "
                + "VALUES (9771, 9771, '수업', '소개', '목표', '본문', 30, 1)");
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9772, 9771, 'CONCEPT', '개념', '본문', 'MARKDOWN', 10, NULL, 1), (9771, 9771, 'QUIZ', '퀴즈', NULL, NULL, 10, 50, 2)");
        jdbcTemplate.update("INSERT INTO concept_tag (concept_tag_id, name) VALUES (9771, '문항관리 태그A'), (9772, '문항관리 태그B')");
        jdbcTemplate.update("INSERT INTO quiz_question (quiz_question_id, activity_id, concept_tag_id, display_type, content, code_snippet, explanation, sort_order) VALUES "
                + "(97711, 9771, 9771, 'CONCEPT', '기록 없는 문항', NULL, '해설', 1), "
                + "(97712, 9771, 9771, 'CONCEPT', '기록 있는 문항', NULL, '해설', 2)");
        jdbcTemplate.update("INSERT INTO quiz_choice (quiz_choice_id, quiz_question_id, content, is_correct, sort_order) VALUES "
                + "(977111, 97711, '가', TRUE, 1), (977112, 97711, '나', FALSE, 2), (977113, 97711, '다', FALSE, 3), (977114, 97711, '라', FALSE, 4), "
                + "(977121, 97712, '가', TRUE, 1), (977122, 97712, '나', FALSE, 2), (977123, 97712, '다', FALSE, 3), (977124, 97712, '라', FALSE, 4)");

        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES ('quiz-admin@vibelog.test', 'x', '문항관리')");
        long memberId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO quiz_attempt (member_id, activity_id, started_at) VALUES (?, 9771, NOW())", memberId);
        long attemptId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("INSERT INTO quiz_answer (quiz_attempt_id, quiz_question_id, quiz_choice_id, is_correct, checked_at) "
                + "VALUES (?, 97712, 977121, TRUE, NOW())", attemptId);
    }

    @Test
    void 문항을_추가하면_마지막_뒤에_사용_중으로_붙고_보기_4개와_정답이_저장된다() {
        Long lessonId = quizAdminService.create(9771L, form(QuizDisplayType.CODE_BLANK, "새 문항", "int a = __;", 2));

        assertThat(lessonId).isEqualTo(9771L);
        AdminQuizQuestionItem added = last(questions());
        assertThat(added.content()).isEqualTo("새 문항");
        assertThat(added.sortOrder()).isEqualTo(3);
        assertThat(added.active()).isTrue();
        assertThat(added.codeSnippet()).isEqualTo("int a = __;");
        assertThat(added.choices()).extracting(AdminQuizQuestionItem.Choice::content).containsExactly("보기1", "보기2", "보기3", "보기4");
        assertThat(added.choices()).extracting(AdminQuizQuestionItem.Choice::correct).containsExactly(false, false, true, false);
    }

    @Test
    void 코드가_필요_없는_유형은_빈_코드를_저장하지_않는다() {
        quizAdminService.create(9771L, form(QuizDisplayType.TRICK, "낚시", " ", 0));
        assertThat(last(questions()).codeSnippet()).isNull();
    }

    @Test
    void 보기_4개_정답_1개_코드_필수_유형을_어기면_저장되지_않는다() {
        assertError(() -> quizAdminService.create(9771L, QuizQuestionForm.of(QuizDisplayType.CONCEPT, "문항", null, "해설", 9771L,
                List.of("가", "나", "다"), 0)), QuizErrorCode.INVALID_INPUT);                        // 보기 3개
        assertError(() -> quizAdminService.create(9771L, QuizQuestionForm.of(QuizDisplayType.CONCEPT, "문항", null, "해설", 9771L,
                List.of("가", "나", "다", "라"), null)), QuizErrorCode.INVALID_INPUT);               // 정답 없음
        assertError(() -> quizAdminService.create(9771L, form(QuizDisplayType.OUTPUT_PREDICT, "문항", null, 0)),
                QuizErrorCode.INVALID_INPUT);                                                     // 코드 필수 유형에 코드 없음
        assertError(() -> quizAdminService.create(9772L, form(QuizDisplayType.CONCEPT, "문항", null, 0)),
                QuizErrorCode.INVALID_INPUT);                                                     // 퀴즈가 아닌 활동
        assertError(() -> quizAdminService.create(9771L, QuizQuestionForm.of(QuizDisplayType.CONCEPT, "문항", null, "해설", 9779L,
                List.of("가", "나", "다", "라"), 0)), QuizErrorCode.INVALID_INPUT);                  // 없는 개념 태그
        assertThat(questions()).hasSize(2);
    }

    @Test
    void 풀이_기록이_없는_문항은_그_자리에서_고친다() {
        quizAdminService.edit(97711L, form(QuizDisplayType.CONCEPT, "고친 문항", null, 3));
        entityManager.flush();

        List<AdminQuizQuestionItem> questions = questions();
        assertThat(questions).hasSize(2);
        AdminQuizQuestionItem edited = questions.get(0);
        assertThat(edited.quizQuestionId()).isEqualTo(97711L);
        assertThat(edited.content()).isEqualTo("고친 문항");
        assertThat(edited.choices()).extracting(AdminQuizQuestionItem.Choice::correct).containsExactly(false, false, false, true);
    }

    @Test
    void 풀이_기록이_있는_문항을_고치면_기존_문항은_중지되고_새_문항으로_저장되며_기존_답은_그대로다() {
        quizAdminService.edit(97712L, form(QuizDisplayType.CONCEPT, "새로 쓴 문항", null, 1));
        entityManager.flush();

        Map<String, Object> old = jdbcTemplate.queryForMap("SELECT content, is_active FROM quiz_question WHERE quiz_question_id = 97712");
        assertThat(old.get("content")).isEqualTo("기록 있는 문항");
        assertThat(old.get("is_active")).isEqualTo(false);
        assertThat(jdbcTemplate.queryForObject("SELECT content FROM quiz_choice WHERE quiz_choice_id = 977121", String.class)).isEqualTo("가");
        assertThat(jdbcTemplate.queryForObject("SELECT quiz_choice_id FROM quiz_answer WHERE quiz_question_id = 97712", Long.class)).isEqualTo(977121L);

        AdminQuizQuestionItem replaced = last(questions());
        assertThat(replaced.quizQuestionId()).isNotEqualTo(97712L);
        assertThat(replaced.content()).isEqualTo("새로 쓴 문항");
        assertThat(replaced.sortOrder()).isEqualTo(2);
        assertThat(replaced.active()).isTrue();
        assertThat(quizReader.countActiveQuestionsOf(9771L)).isEqualTo(2);
    }

    @Test
    void 사용_중지하고_다시_사용할_수_있다() {
        assertThat(quizAdminService.changeActive(97711L, false)).isEqualTo(9771L);
        entityManager.flush();
        assertThat(quizReader.countActiveQuestionsOf(9771L)).isEqualTo(1);

        quizAdminService.changeActive(97711L, true);
        entityManager.flush();
        assertThat(quizReader.countActiveQuestionsOf(9771L)).isEqualTo(2);
        assertError(() -> quizAdminService.changeActive(97719L, true), QuizErrorCode.INVALID_INPUT);
    }

    private List<AdminQuizQuestionItem> questions() {
        entityManager.flush();
        entityManager.clear();
        return quizReader.getAdminQuestionsOf(List.of(9771L));
    }

    private AdminQuizQuestionItem last(List<AdminQuizQuestionItem> questions) {
        return questions.get(questions.size() - 1);
    }

    private QuizQuestionForm form(QuizDisplayType type, String content, String code, int correctIndex) {
        return QuizQuestionForm.of(type, content, code, "해설", 9772L, List.of("보기1", "보기2", "보기3", "보기4"), correctIndex);
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(expected);
    }
}
