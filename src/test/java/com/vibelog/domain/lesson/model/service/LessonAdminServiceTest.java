package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.LessonEditView;
import com.vibelog.domain.lesson.model.dto.LessonFormRequest;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 운영자 수업 만들기·순서·편집(API-ADM-17~20, FR-LSN-011, #100)을 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 코스·수업·활동·문항은 테스트 안에서만 넣는다. 끝나면 모두 롤백된다(@Transactional).
 *
 * 테스트 코스 구조 (코스 9401)
 *  - 수업 1(9401, 공개, 순서 1): 개념 활동(9401, 사용) · 퀴즈 활동(9402, 중지) · 모의고사 문항(9401, 보기 2개)
 *  - 수업 2(9402, 비공개, 순서 2)
 *  - 수업 3(9403, 비공개, 순서 3)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9401, '테스트 트랙', '대상', '설명', 9401)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9401, '테스트 카테고리', 9401)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9401, 9401, 9401, '코스 9401', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9401, 9401, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9402, 9401, '수업 2', '소개', '목표', '본문', 20, 2, FALSE), "
                + "(9403, 9401, '수업 3', '소개', '목표', '본문', 10, 3, FALSE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order, is_active) VALUES "
                + "(9401, 9401, 'CONCEPT', '개념 1-1', '본문', 'MARKDOWN', 10, NULL, 1, TRUE), "
                + "(9402, 9401, 'QUIZ', '퀴즈 1-1', NULL, NULL, 10, 50, 2, FALSE)",
        "INSERT INTO concept_tag (concept_tag_id, name) VALUES (9401, '변수'), (9402, '조건문')",
        "INSERT INTO exam_question (exam_question_id, lesson_id, concept_activity_id, content, explanation, sort_order, is_active) VALUES "
                + "(9401, 9401, 9401, '문항 내용', '해설', 1, TRUE)",
        "INSERT INTO exam_choice (exam_choice_id, exam_question_id, content, is_correct, sort_order) VALUES "
                + "(9401, 9401, '보기1', TRUE, 1), (9402, 9401, '보기2', FALSE, 2)"
})
class LessonAdminServiceTest {

    static final long COURSE = 9401L;
    static final long LESSON_1 = 9401L;
    static final long LESSON_2 = 9402L;
    static final long LESSON_3 = 9403L;

    @Autowired LessonAdminService lessonAdminService;
    @Autowired LessonRepository lessonRepository;

    @Test
    void 수업을_만들면_비공개로_저장되고_순서는_마지막_다음이다() {
        Long lessonId = lessonAdminService.create(COURSE, formOf("새 수업"));

        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(lesson.getTitle()).isEqualTo("새 수업");
        assertThat(lesson.isPublic()).isFalse();
        assertThat(lesson.getSortOrder()).isEqualTo(4);
    }

    @Test
    void 없는_코스에_수업을_만들면_COURSE_NOT_FOUND() {
        assertThatThrownBy(() -> lessonAdminService.create(999_999L, formOf("새 수업")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void 수업_순서를_바꾸면_그_순서가_저장된다() {
        lessonAdminService.reorder(COURSE, List.of(LESSON_3, LESSON_1, LESSON_2));

        assertThat(lessonRepository.findById(LESSON_3).orElseThrow().getSortOrder()).isEqualTo(1);
        assertThat(lessonRepository.findById(LESSON_1).orElseThrow().getSortOrder()).isEqualTo(2);
        assertThat(lessonRepository.findById(LESSON_2).orElseThrow().getSortOrder()).isEqualTo(3);
    }

    @Test
    void 순서_목록에서_수업이_빠지면_INVALID_INPUT이고_순서는_그대로다() {
        assertThatThrownBy(() -> lessonAdminService.reorder(COURSE, List.of(LESSON_1, LESSON_2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.INVALID_INPUT);
        assertThat(lessonRepository.findById(LESSON_1).orElseThrow().getSortOrder()).isEqualTo(1);
    }

    @Test
    void 순서_목록에_다른_코스_수업이_섞이면_INVALID_INPUT이다() {
        assertThatThrownBy(() -> lessonAdminService.reorder(COURSE, List.of(LESSON_1, LESSON_2, 999_999L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.INVALID_INPUT);
    }

    @Test
    void 수업_정보를_수정하면_바뀐_값이_저장된다() {
        lessonAdminService.update(LESSON_1, formOf("바뀐 제목"));

        Lesson lesson = lessonRepository.findById(LESSON_1).orElseThrow();
        assertThat(lesson.getTitle()).isEqualTo("바뀐 제목");
        assertThat(lesson.getEstimatedMinutes()).isEqualTo(15);
    }

    @Test
    void 없는_수업을_수정하면_LESSON_NOT_FOUND() {
        assertThatThrownBy(() -> lessonAdminService.update(999_999L, formOf("제목")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.LESSON_NOT_FOUND);
    }

    @Test
    void 편집_화면_데이터에_활동_모의고사_문항_개념_태그가_모두_담긴다() {
        LessonEditView view = lessonAdminService.getEditView(LESSON_1, null);

        assertThat(view.courseTitle()).isEqualTo("코스 9401");
        assertThat(view.tab()).isEqualTo("activities");
        assertThat(view.activities()).hasSize(2);
        assertThat(view.activities()).anySatisfy(a -> {
            assertThat(a.title()).isEqualTo("퀴즈 1-1");
            assertThat(a.active()).isFalse();
        });
        assertThat(view.conceptTags()).extracting("name").contains("변수", "조건문");
        assertThat(view.examQuestions()).hasSize(1);
        assertThat(view.examQuestions().get(0).content()).isEqualTo("문항 내용");
        assertThat(view.examQuestions().get(0).choices()).hasSize(2);
    }

    @Test
    void tab_쿼리가_exam이면_exam_탭이_활성화된다() {
        LessonEditView view = lessonAdminService.getEditView(LESSON_1, "exam");

        assertThat(view.tab()).isEqualTo("exam");
    }

    private LessonFormRequest formOf(String title) {
        LessonFormRequest request = new LessonFormRequest();
        request.setTitle(title);
        request.setIntro("소개");
        request.setGoal("목표");
        request.setContents("본문");
        request.setEstimatedMinutes(15);
        return request;
    }
}
