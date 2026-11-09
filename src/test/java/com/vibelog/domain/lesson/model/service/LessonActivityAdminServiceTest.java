package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.ActivityTagRepository;
import com.vibelog.domain.lesson.model.dto.ActivityFormRequest;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.entity.BodyFormat;
import com.vibelog.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 운영자 활동 추가·수정·사용 중지·순서(API-ADM-21~24, FR-LSN-011, PL-LSN-001, #101)를 실제 DB(01·02 실행된
 * 로컬 MySQL)로 확인한다. 코스·수업·활동·수강·진도는 테스트 안에서만 넣는다. 끝나면 모두 롤백된다(@Transactional).
 *
 * 테스트 코스 구조 (코스 9501)
 *  - 수업 1(9501, 공개, 순서 1): 개념(9501,사용,1) → 퀴즈(9502,사용,2) → 개념(9503,사용,3) → 퀴즈(9504,중지,4)
 *    → 퀴즈(9505,사용,5). 사용 중인 활동만 보면 개념·퀴즈·개념·퀴즈(9501,9502,9503,9505)로 짝이 맞는다.
 *  - 수강(9501, 학습자 1) · 활동 진도(9501, 활동 9502, 완료)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9501, '테스트 트랙', '대상', '설명', 9501)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9501, '테스트 카테고리', 9501)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9501, 9501, 9501, '코스 9501', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9501, 9501, '수업 1', '소개', '목표', '본문', 30, 1, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order, is_active) VALUES "
                + "(9501, 9501, 'CONCEPT', '개념 1', '본문', 'MARKDOWN', 10, NULL, 1, TRUE), "
                + "(9502, 9501, 'QUIZ', '퀴즈 1', NULL, NULL, 10, 50, 2, TRUE), "
                + "(9503, 9501, 'CONCEPT', '개념 2', '본문', 'MARKDOWN', 10, NULL, 3, TRUE), "
                + "(9504, 9501, 'QUIZ', '퀴즈 2(중지)', NULL, NULL, 10, 50, 4, FALSE), "
                + "(9505, 9501, 'QUIZ', '퀴즈 3', NULL, NULL, 10, 50, 5, TRUE)",
        "INSERT INTO concept_tag (concept_tag_id, name) VALUES (9501, '변수')",
        "INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at) VALUES (9501, 1, 9501, NOW())",
        "INSERT INTO activity_progress (activity_progress_id, enrollment_id, activity_id, unlocked_at, completed_at) VALUES "
                + "(9501, 9501, 9502, NOW(), NOW())"
})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class LessonActivityAdminServiceTest {

    static final long LESSON = 9501L;
    static final long CONCEPT_1 = 9501L;
    static final long QUIZ_1 = 9502L;
    static final long CONCEPT_2 = 9503L;
    static final long QUIZ_2_INACTIVE = 9504L;
    static final long QUIZ_3 = 9505L;

    @Autowired LessonActivityAdminService lessonActivityAdminService;
    @Autowired ActivityRepository activityRepository;
    @Autowired ActivityTagRepository activityTagRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;

    @Test
    void 활동을_추가하면_전체_활동_중_마지막_순서_다음으로_저장된다() {
        Long activityId = lessonActivityAdminService.addActivity(LESSON, formOf(ActivityType.CONCEPT, "개념 3", "본문"));

        Activity activity = activityRepository.findById(activityId).orElseThrow();
        assertThat(activity.getSortOrder()).isEqualTo(6); // 사용 중지된 9504 포함 전체 5개 다음
        assertThat(activity.getBodyFormat()).isEqualTo(BodyFormat.MARKDOWN);
        assertThat(activity.isActive()).isTrue();
    }

    @Test
    void 본문이_없으면_bodyFormat은_null로_저장된다() {
        Long activityId = lessonActivityAdminService.addActivity(LESSON, formOf(ActivityType.QUIZ, "퀴즈 4", null));

        assertThat(activityRepository.findById(activityId).orElseThrow().getBodyFormat()).isNull();
    }

    @Test
    void 퀴즈를_추가하면_통과_기준이_50퍼센트로_저장된다() {
        Long quizId = lessonActivityAdminService.addActivity(LESSON, formOf(ActivityType.QUIZ, "퀴즈 4", null));
        Long conceptId = lessonActivityAdminService.addActivity(LESSON, formOf(ActivityType.CONCEPT, "개념 3", "본문"));

        assertThat(activityRepository.findById(quizId).orElseThrow().getPassRate()).isEqualTo(50);
        assertThat(activityRepository.findById(conceptId).orElseThrow().getPassRate()).isNull();
    }

    @Test
    void 태그를_포함해_추가하면_활동_태그가_함께_저장된다() {
        ActivityFormRequest form = formOf(ActivityType.CONCEPT, "개념 3", "본문");
        form.setTagNames(List.of("변수", "새태그"));

        Long activityId = lessonActivityAdminService.addActivity(LESSON, form);

        assertThat(activityTagRepository.findByActivityId(activityId)).hasSize(2);
    }

    @Test
    void 없는_수업에_활동을_추가하면_LESSON_NOT_FOUND() {
        assertThatThrownBy(() -> lessonActivityAdminService.addActivity(999_999L, formOf(ActivityType.CONCEPT, "개념", "본문")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.LESSON_NOT_FOUND);
    }

    @Test
    void 활동을_수정하면_값과_태그가_바뀐다() {
        ActivityFormRequest form = formOf(ActivityType.CONCEPT, "바뀐 제목", "바뀐 본문");
        form.setTagNames(List.of("변수"));

        Long lessonId = lessonActivityAdminService.editActivity(CONCEPT_1, form);

        assertThat(lessonId).isEqualTo(LESSON);
        Activity activity = activityRepository.findById(CONCEPT_1).orElseThrow();
        assertThat(activity.getTitle()).isEqualTo("바뀐 제목");
        assertThat(activity.getBody()).isEqualTo("바뀐 본문");
        assertThat(activityTagRepository.findByActivityId(CONCEPT_1)).hasSize(1);
    }

    @Test
    void 활동을_수정해도_활동_종류는_바뀌지_않는다() {
        ActivityFormRequest form = formOf(ActivityType.QUIZ, "바뀐 제목", "바뀐 본문");

        lessonActivityAdminService.editActivity(CONCEPT_1, form);

        assertThat(activityRepository.findById(CONCEPT_1).orElseThrow().getActivityType()).isEqualTo(ActivityType.CONCEPT);
    }

    @Test
    @Sql(statements = "INSERT INTO activity_tag (activity_tag_id, activity_id, concept_tag_id) VALUES (9501, 9501, 9501)")
    void 기존_태그와_같은_태그로_수정해도_유니크_제약에_걸리지_않는다() {
        ActivityFormRequest form = formOf(ActivityType.CONCEPT, "바뀐 제목", "본문");
        form.setTagNames(List.of("변수"));

        lessonActivityAdminService.editActivity(CONCEPT_1, form);

        assertThat(activityTagRepository.findByActivityId(CONCEPT_1)).hasSize(1);
    }

    @Test
    void 없는_활동을_수정하면_ACTIVITY_NOT_FOUND() {
        assertThatThrownBy(() -> lessonActivityAdminService.editActivity(999_999L, formOf(ActivityType.CONCEPT, "제목", "본문")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.ACTIVITY_NOT_FOUND);
    }

    @Test
    void 활동을_사용_중지해도_학습자_진도_기록은_그대로_남는다() {
        Long lessonId = lessonActivityAdminService.deactivateActivity(QUIZ_1);

        assertThat(lessonId).isEqualTo(LESSON);
        assertThat(activityRepository.findById(QUIZ_1).orElseThrow().isActive()).isFalse();
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(9501L, QUIZ_1)).isPresent();
    }

    @Test
    void 개념_뒤에_퀴즈가_오지_않는_순서로_저장하면_INVALID_ORDER() {
        // 사용 중인 활동만 보면 개념,개념,퀴즈,퀴즈 순서가 되어 PL-LSN-001(개념 뒤에 퀴즈 하나씩)을 어긴다
        assertThatThrownBy(() -> lessonActivityAdminService.reorderActivities(
                LESSON, List.of(CONCEPT_1, CONCEPT_2, QUIZ_1, QUIZ_2_INACTIVE, QUIZ_3)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.INVALID_ORDER);
        // 순서가 바뀌지 않아야 한다
        assertThat(activityRepository.findById(CONCEPT_1).orElseThrow().getSortOrder()).isEqualTo(1);
        assertThat(activityRepository.findById(QUIZ_3).orElseThrow().getSortOrder()).isEqualTo(5);
    }

    @Test
    void 사용_중지된_활동은_순서_검사에서_빠지고_나머지가_번갈아_오면_저장된다() {
        // 사용 중지된 퀴즈 2(9504)를 맨 뒤로 보내도, 사용 중인 활동만 보면 개념,퀴즈,개념,퀴즈로 짝이 맞는다
        lessonActivityAdminService.reorderActivities(
                LESSON, List.of(CONCEPT_2, QUIZ_3, CONCEPT_1, QUIZ_1, QUIZ_2_INACTIVE));

        assertThat(activityRepository.findById(CONCEPT_2).orElseThrow().getSortOrder()).isEqualTo(1);
        assertThat(activityRepository.findById(QUIZ_3).orElseThrow().getSortOrder()).isEqualTo(2);
        assertThat(activityRepository.findById(CONCEPT_1).orElseThrow().getSortOrder()).isEqualTo(3);
        assertThat(activityRepository.findById(QUIZ_1).orElseThrow().getSortOrder()).isEqualTo(4);
        assertThat(activityRepository.findById(QUIZ_2_INACTIVE).orElseThrow().getSortOrder()).isEqualTo(5);
    }

    @Test
    void 순서_목록에서_활동이_빠지면_INVALID_INPUT이고_순서는_그대로다() {
        assertThatThrownBy(() -> lessonActivityAdminService.reorderActivities(LESSON, List.of(CONCEPT_1, QUIZ_1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.INVALID_INPUT);
        assertThat(activityRepository.findById(CONCEPT_1).orElseThrow().getSortOrder()).isEqualTo(1);
    }

    @Test
    void 이미지가_아닌_파일을_올리면_실패한다() {
        MockMultipartFile notImage = new MockMultipartFile(
                "image", "virus.exe", "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> lessonActivityAdminService.uploadConceptImage(notImage))
                .isInstanceOf(BusinessException.class);
    }

    private ActivityFormRequest formOf(ActivityType type, String title, String body) {
        ActivityFormRequest request = new ActivityFormRequest();
        request.setActivityType(type);
        request.setTitle(title);
        request.setBody(body);
        request.setEstimatedMinutes(10);
        return request;
    }
}
