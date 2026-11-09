package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.dao.ExamChoiceRepository;
import com.vibelog.domain.exam.model.dao.ExamQuestionRepository;
import com.vibelog.domain.exam.model.dto.ExamQuestionForm;
import com.vibelog.domain.exam.model.entity.ExamChoice;
import com.vibelog.domain.exam.model.entity.ExamQuestion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/*
 * 운영자 모의고사 문항 추가·수정·사용 여부 (API-ADM-29~31, FR-EXM-007, PL-EXM-002). 테스트가 끝나면 롤백된다.
 *
 * 수업 9301: 개념 9301, 퀴즈 9302. 수업 9302: 개념 9303.
 * 문항 9301(수업 9301, 보기 2개뿐인 예전 데이터: 9311 정답, 9312 오답)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9301, '테스트트랙', '대상', '설명', 9301)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9301, '테스트분류', 9301)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES (9301, 9301, 9301, '코스', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order) VALUES "
                + "(9301, 9301, '수업1', '소개', '목표', '내용', 10, 1), (9302, 9301, '수업2', '소개', '목표', '내용', 10, 2)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES "
                + "(9301, 9301, 'CONCEPT', '개념', '본문', 'MARKDOWN', 10, NULL, 1), (9302, 9301, 'QUIZ', '퀴즈', NULL, NULL, 10, 50, 2), "
                + "(9303, 9302, 'CONCEPT', '다른 수업 개념', '본문', 'MARKDOWN', 10, NULL, 1)",
        "INSERT INTO exam_question (exam_question_id, lesson_id, concept_activity_id, content, explanation, sort_order, is_active) VALUES "
                + "(9301, 9301, 9301, '예전 문항', '예전 해설', 1, TRUE)",
        "INSERT INTO exam_choice (exam_choice_id, exam_question_id, content, is_correct, sort_order) VALUES "
                + "(9311, 9301, '예전 정답', TRUE, 1), (9312, 9301, '예전 오답', FALSE, 2)"
})
class ExamAdminServiceTest {

    static final Long LESSON = 9301L;
    static final Long CONCEPT = 9301L;

    @Autowired ExamAdminService examAdminService;
    @Autowired ExamQuestionRepository examQuestionRepository;
    @Autowired ExamChoiceRepository examChoiceRepository;
    @Autowired EntityManager entityManager;

    private ExamQuestionForm form(Long conceptActivityId, int correctIndex) {
        ExamQuestionForm form = new ExamQuestionForm();
        form.setContent("새 문항");
        form.setExplanation("새 해설");
        form.setConceptActivityId(conceptActivityId);
        form.setChoices(List.of("가", "나", "다", "라"));
        form.setCorrectIndex(correctIndex);
        return form;
    }

    private List<ExamChoice> choicesOf(Long examQuestionId) {
        entityManager.flush();
        entityManager.clear();
        return examChoiceRepository.findByExamQuestion_ExamQuestionIdOrderBySortOrderAsc(examQuestionId);
    }

    @Test
    void 추가하면_마지막_문항_뒤에_사용_중으로_붙고_보기_4개와_정답이_저장된다() {
        Long id = examAdminService.create(LESSON, form(CONCEPT, 2));

        ExamQuestion question = examQuestionRepository.findById(id).orElseThrow();
        assertThat(question.getSortOrder()).isEqualTo(2);
        assertThat(question.isActive()).isTrue();
        assertThat(question.getConceptActivityId()).isEqualTo(CONCEPT);
        assertThat(choicesOf(id)).extracting(ExamChoice::getContent, ExamChoice::isCorrect, ExamChoice::getSortOrder)
                .containsExactly(tuple("가", false, 1), tuple("나", false, 2), tuple("다", true, 3), tuple("라", false, 4));
    }

    @Test
    void 연관_개념이_없으면_CONCEPT_REQUIRED() {
        assertThatThrownBy(() -> examAdminService.create(LESSON, form(null, 0)))
                .extracting("errorCode").isEqualTo(ExamErrorCode.CONCEPT_REQUIRED);
    }

    @Test
    void 연관_개념은_그_수업의_개념_활동만_고를_수_있다() {
        assertThatThrownBy(() -> examAdminService.create(LESSON, form(9302L, 0)))   // 같은 수업의 퀴즈
                .extracting("errorCode").isEqualTo(ExamErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> examAdminService.create(LESSON, form(9303L, 0)))   // 다른 수업의 개념
                .extracting("errorCode").isEqualTo(ExamErrorCode.INVALID_INPUT);
    }

    @Test
    void 수정하면_있던_보기는_그_자리에서_고치고_모자란_보기는_더한다() {
        Long lessonId = examAdminService.edit(9301L, form(CONCEPT, 1));

        assertThat(lessonId).isEqualTo(LESSON);
        List<ExamChoice> choices = choicesOf(9301L);
        assertThat(choices).extracting(ExamChoice::getContent, ExamChoice::isCorrect)
                .containsExactly(tuple("가", false), tuple("나", true), tuple("다", false), tuple("라", false));
        // 응시 기록이 가리킬 수 있는 예전 보기 9311·9312 는 지우지 않고 그대로 고쳤다
        assertThat(choices).extracting(ExamChoice::getExamChoiceId).startsWith(9311L, 9312L);
        ExamQuestion question = examQuestionRepository.findById(9301L).orElseThrow();
        assertThat(question.getContent()).isEqualTo("새 문항");
        assertThat(question.getSortOrder()).isEqualTo(1);
    }

    @Test
    void 사용_여부를_바꾸고_돌아갈_수업을_알려_준다() {
        assertThat(examAdminService.changeActive(9301L, false)).isEqualTo(LESSON);

        assertThat(examQuestionRepository.findById(9301L).orElseThrow().isActive()).isFalse();
    }

    @Test
    void 없는_문항은_INVALID_INPUT() {
        assertThatThrownBy(() -> examAdminService.changeActive(99999L, false))
                .extracting("errorCode").isEqualTo(ExamErrorCode.INVALID_INPUT);
    }
}
