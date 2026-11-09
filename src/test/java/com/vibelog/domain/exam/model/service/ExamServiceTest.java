package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.dao.ExamAnswerRepository;
import com.vibelog.domain.exam.model.dao.ExamAttemptRepository;
import com.vibelog.domain.exam.model.dto.ExamPageView;
import com.vibelog.domain.exam.model.dto.ExamStartResult;
import com.vibelog.domain.exam.model.dto.ExamSubmitResult;
import com.vibelog.domain.exam.model.entity.ExamAnswer;
import com.vibelog.domain.exam.model.entity.ExamAttempt;
import com.vibelog.domain.lesson.model.service.LessonContractService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.willReturn;

/*
 * 모의고사 응시·답 저장·채점 규칙 (API-EXM-01~04, PL-EXM-003·006).
 * 열림 여부(계약 ②)는 lesson 영역 몫이라 가짜로 두고, exam 테이블은 실제 DB 에 넣는다 (테스트가 끝나면 롤백).
 *
 * 수업 9101 의 문항: 9101(정답 9111, 오답 9112), 9102(오답 9121, 정답 9122), 9103(사용 중지, 정답 9131)
 * 회원 9101 이 응시하고, 회원 9102 는 남의 응시를 건드린다.
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES (9101, 'exam-svc-a@vibelog.test', '응시A'), (9102, 'exam-svc-b@vibelog.test', '응시B')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9101, '테스트트랙', '대상', '설명', 9101)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9101, '테스트분류', 9101)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES (9101, 9101, 9101, '코스', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order) VALUES (9101, 9101, '수업1', '소개', '목표', '내용', 10, 1)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES (9101, 9101, 'CONCEPT', '개념', '본문', 'MARKDOWN', 10, NULL, 1)",
        "INSERT INTO exam_question (exam_question_id, lesson_id, concept_activity_id, content, explanation, sort_order, is_active) VALUES "
                + "(9101, 9101, 9101, '문항1', '해설1', 1, TRUE), (9102, 9101, 9101, '문항2', '해설2', 2, TRUE), (9103, 9101, 9101, '문항3', '해설3', 3, FALSE)",
        "INSERT INTO exam_choice (exam_choice_id, exam_question_id, content, is_correct, sort_order) VALUES "
                + "(9111, 9101, '정답', TRUE, 1), (9112, 9101, '오답', FALSE, 2), "
                + "(9121, 9102, '오답', FALSE, 1), (9122, 9102, '정답', TRUE, 2), (9131, 9103, '정답', TRUE, 1)"
})
class ExamServiceTest {

    static final Long MEMBER = 9101L;
    static final Long OTHER = 9102L;
    static final Long LESSON = 9101L;

    @Autowired ExamService examService;
    @Autowired ExamAttemptRepository examAttemptRepository;
    @Autowired ExamAnswerRepository examAnswerRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    // 계약 ② 구현 빈은 다른 계약도 같이 구현하므로 통째로 바꾸지 않고 isExamOpen 만 바꿔 둔다
    @MockitoSpyBean LessonContractService examLessonProgress;

    @BeforeEach
    void examIsOpen() {
        willReturn(true).given(examLessonProgress).isExamOpen(MEMBER, LESSON);
    }

    private ExamAttempt latest() {
        return examAttemptRepository.findFirstByMemberIdAndLessonIdOrderByAttemptNoDesc(MEMBER, LESSON).orElseThrow();
    }

    @Test
    void 마지막_퀴즈를_통과하지_않았으면_응시를_만들지_않는다() {
        willReturn(false).given(examLessonProgress).isExamOpen(MEMBER, LESSON);

        assertThat(examService.start(MEMBER, LESSON).status()).isEqualTo(ExamStartResult.Status.LOCKED);
        assertThat(examAttemptRepository.findFirstByMemberIdAndLessonIdOrderByAttemptNoDesc(MEMBER, LESSON)).isEmpty();
    }

    @Test
    void 처음이면_1회차를_만들고_제출_전에_다시_부르면_같은_회차를_이어_푼다() {
        examService.start(MEMBER, LESSON);
        examService.start(MEMBER, LESSON);

        assertThat(latest().getAttemptNo()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_attempt WHERE member_id = 9101", Integer.class)).isEqualTo(1);
    }

    @Test
    void 제출하고_리포트를_보기_전에는_다시_응시할_수_없다() {
        examService.start(MEMBER, LESSON);
        examService.submit(MEMBER, LESSON, latest().getExamAttemptId(), Map.of());

        ExamStartResult result = examService.start(MEMBER, LESSON);

        assertThat(result.status()).isEqualTo(ExamStartResult.Status.REPORT_NOT_VIEWED);
        assertThat(result.attemptNo()).isEqualTo(1);
    }

    @Test
    void 리포트를_봤으면_다음_회차를_만든다() {
        examService.start(MEMBER, LESSON);
        examService.submit(MEMBER, LESSON, latest().getExamAttemptId(), Map.of());
        entityManager.flush();
        // 리포트 확인 기록은 #111 몫이라 여기서는 DB 에 직접 남기고, 다시 읽도록 1차 캐시를 비운다
        jdbcTemplate.update("UPDATE exam_attempt SET report_viewed_at = NOW() WHERE member_id = 9101");
        entityManager.clear();

        examService.start(MEMBER, LESSON);

        assertThat(latest().getAttemptNo()).isEqualTo(2);
    }

    @Test
    void 답을_바꾸고_다시_누르면_선택이_취소된다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();

        examService.saveAnswer(MEMBER, attemptId, 9101L, 9112L);
        examService.saveAnswer(MEMBER, attemptId, 9101L, 9111L);
        assertThat(selectedOf(9101L)).isEqualTo(9111L);

        examService.saveAnswer(MEMBER, attemptId, 9101L, null);
        assertThat(selectedOf(9101L)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_answer WHERE exam_attempt_id = ?", Integer.class, attemptId)).isEqualTo(1);
    }

    @Test
    void 화면에는_사용_중인_문항만_순서대로_나온다() {
        examService.start(MEMBER, LESSON);

        ExamPageView page = examService.getPage(MEMBER, LESSON).orElseThrow();

        assertThat(page.questions()).extracting(ExamPageView.Question::examQuestionId).containsExactly(9101L, 9102L);
        assertThat(page.questions().get(1).choices()).extracting(ExamPageView.Choice::examChoiceId).containsExactly(9121L, 9122L);
        assertThat(page.courseId()).isEqualTo(9101L);
        assertThat(page.lessonTitle()).isEqualTo("수업1");
    }

    @Test
    void 남의_응시에는_답을_저장할_수_없다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();

        assertThatThrownBy(() -> examService.saveAnswer(OTHER, attemptId, 9101L, 9111L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(CommonErrorCode.NOT_OWNER);
    }

    @Test
    void 다른_문항의_보기나_사용_중지된_문항은_저장할_수_없다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();

        assertThatThrownBy(() -> examService.saveAnswer(MEMBER, attemptId, 9101L, 9122L))
                .extracting("errorCode").isEqualTo(ExamErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> examService.saveAnswer(MEMBER, attemptId, 9103L, 9131L))
                .extracting("errorCode").isEqualTo(ExamErrorCode.INVALID_INPUT);
    }

    @Test
    void 제출하면_맞힌_수와_정답률을_남기고_고르지_않은_문항은_오답이다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();
        examService.saveAnswer(MEMBER, attemptId, 9101L, 9111L);   // 정답

        ExamSubmitResult result = examService.submit(MEMBER, LESSON, attemptId, Map.of(9102L, 9121L)); // 오답

        ExamAttempt attempt = latest();
        assertThat(result).isEqualTo(new ExamSubmitResult(1, false));
        assertThat(attempt.isSubmitted()).isTrue();
        assertThat(attempt.getScore()).isEqualTo(1);
        assertThat(attempt.getCorrectRate()).isEqualByComparingTo("50.00");
        assertThat(examAnswerRepository.findByExamAttempt(attempt))
                .extracting(a -> a.getExamQuestion().getExamQuestionId(), ExamAnswer::getCorrect)
                .containsExactlyInAnyOrder(
                        tuple(9101L, true),
                        tuple(9102L, false));
    }

    @Test
    void 제출할_때_빈_값으로_온_문항은_저장된_이전_답을_취소하고_오답으로_채점한다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();
        examService.saveAnswer(MEMBER, attemptId, 9101L, 9111L);   // 정답을 골라 둠 (취소 저장은 실패했다고 가정)
        Map<Long, Long> lastAnswers = new HashMap<>();
        lastAnswers.put(9101L, null);                                // 화면에서는 미선택

        examService.submit(MEMBER, LESSON, attemptId, lastAnswers);

        assertThat(latest().getScore()).isZero();
        assertThat(examAnswerRepository.findByExamAttempt(latest()))
                .filteredOn(a -> a.getExamQuestion().getExamQuestionId().equals(9101L))
                .singleElement().satisfies(a -> {
                    assertThat(a.getExamChoice()).isNull();
                    assertThat(a.getCorrect()).isFalse();
                });
    }

    @Test
    void 응시_중에_사용_중지된_문항이_제출_폼에_남아_있어도_건너뛰고_채점한다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();

        // 9103 은 사용 중지된 문항 (응시를 시작한 뒤 운영자가 끈 경우와 같다)
        ExamSubmitResult result = examService.submit(MEMBER, LESSON, attemptId, Map.of(9101L, 9111L, 9103L, 9131L));

        assertThat(result.alreadySubmitted()).isFalse();
        assertThat(latest().getScore()).isEqualTo(1);
        assertThat(examAnswerRepository.findByExamAttempt(latest()))
                .extracting(a -> a.getExamQuestion().getExamQuestionId())
                .containsExactlyInAnyOrder(9101L, 9102L);
    }

    @Test
    void 답을_저장한_뒤_문항이_사용_중지되면_제출할_때_그_답은_지우고_채점한다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();
        examService.saveAnswer(MEMBER, attemptId, 9101L, 9111L);   // 정답
        examService.saveAnswer(MEMBER, attemptId, 9102L, 9122L);   // 정답
        jdbcTemplate.update("UPDATE exam_question SET is_active = FALSE WHERE exam_question_id = 9102");   // 운영자가 사용 중지
        entityManager.flush();
        entityManager.clear();

        examService.submit(MEMBER, LESSON, attemptId, Map.of(9101L, 9111L, 9102L, 9122L));

        assertThat(latest().getScore()).isEqualTo(1);
        assertThat(latest().getCorrectRate()).isEqualByComparingTo("100.00");
        assertThat(examAnswerRepository.findByExamAttempt(latest()))
                .extracting(a -> a.getExamQuestion().getExamQuestionId(), ExamAnswer::getCorrect)
                .containsExactly(tuple(9101L, true));
    }

    @Test
    void 아무것도_고르지_않고_제출하면_모든_문항이_오답으로_남는다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();

        examService.submit(MEMBER, LESSON, attemptId, Map.of());

        assertThat(latest().getScore()).isZero();
        assertThat(latest().getCorrectRate()).isEqualByComparingTo("0.00");
        assertThat(examAnswerRepository.findByExamAttempt(latest()))
                .hasSize(2).allSatisfy(a -> assertThat(a.getCorrect()).isFalse());
    }

    @Test
    void 이미_제출한_응시는_다시_채점하지_않고_답도_바꿀_수_없다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();
        examService.submit(MEMBER, LESSON, attemptId, Map.of(9101L, 9111L));

        ExamSubmitResult again = examService.submit(MEMBER, LESSON, attemptId, Map.of(9102L, 9122L));

        assertThat(again.alreadySubmitted()).isTrue();
        assertThat(latest().getScore()).isEqualTo(1);
        assertThatThrownBy(() -> examService.saveAnswer(MEMBER, attemptId, 9102L, 9122L))
                .extracting("errorCode").isEqualTo(ExamErrorCode.ATTEMPT_SUBMITTED);
    }

    @Test
    void 수강을_포기해_모의고사가_닫히면_답_저장과_제출이_막히고_저장되지_않는다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();
        examService.saveAnswer(MEMBER, attemptId, 9101L, 9112L);
        willReturn(false).given(examLessonProgress).isExamOpen(MEMBER, LESSON);   // 수강 포기 → 계약 ② 가 닫힘으로 답한다

        assertThatThrownBy(() -> examService.saveAnswer(MEMBER, attemptId, 9101L, 9111L))
                .extracting("errorCode").isEqualTo(ExamErrorCode.EXAM_LOCKED);
        assertThatThrownBy(() -> examService.submit(MEMBER, LESSON, attemptId, Map.of(9102L, 9122L)))
                .extracting("errorCode").isEqualTo(ExamErrorCode.EXAM_LOCKED);

        entityManager.flush();
        entityManager.clear();
        assertThat(latest().isSubmitted()).isFalse();
        assertThat(examAnswerRepository.findByExamAttempt(latest()))
                .extracting(a -> a.getExamChoice().getExamChoiceId()).containsExactly(9112L);
    }

    @Test
    void 다시_수강한_뒤에는_포기_전에_시작한_응시를_이어_저장하고_제출할_수_있다() {
        examService.start(MEMBER, LESSON);
        Long attemptId = latest().getExamAttemptId();
        willReturn(false).given(examLessonProgress).isExamOpen(MEMBER, LESSON);
        assertThatThrownBy(() -> examService.saveAnswer(MEMBER, attemptId, 9101L, 9111L))
                .extracting("errorCode").isEqualTo(ExamErrorCode.EXAM_LOCKED);
        willReturn(true).given(examLessonProgress).isExamOpen(MEMBER, LESSON);   // 재시작·복습 등으로 다시 열림

        examService.saveAnswer(MEMBER, attemptId, 9101L, 9111L);
        ExamSubmitResult result = examService.submit(MEMBER, LESSON, attemptId, Map.of());

        assertThat(result.alreadySubmitted()).isFalse();
        assertThat(latest().getScore()).isEqualTo(1);
    }

    @Test
    void 정답률은_소수_둘째_자리에서_반올림한다() {
        assertThat(ExamService.correctRate(1, 3)).isEqualByComparingTo(new BigDecimal("33.33"));
        assertThat(ExamService.correctRate(2, 3)).isEqualByComparingTo(new BigDecimal("66.67"));
        assertThat(ExamService.correctRate(0, 0)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Long selectedOf(Long questionId) {
        return examService.getPage(MEMBER, LESSON).orElseThrow().questions().stream()
                .filter(q -> q.examQuestionId().equals(questionId))
                .findFirst().orElseThrow().selectedChoiceId();
    }
}
