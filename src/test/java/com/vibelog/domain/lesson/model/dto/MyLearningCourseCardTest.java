package com.vibelog.domain.lesson.model.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/* "학습하기"가 보낼 수업(설계서 U-12): 진행 중인 수업 → 없으면 열린 수업 중 마지막 → 없으면 null */
class MyLearningCourseCardTest {

    private MyLearningLessonCard lesson(long id, LessonStatus status) {
        LessonExamStatus exam = status == LessonStatus.LOCKED ? LessonExamStatus.LOCKED : LessonExamStatus.OPEN;
        return new MyLearningLessonCard(id, (int) id, "수업 " + id, status, exam);
    }

    private MyLearningCourseCard course(List<MyLearningLessonCard> lessons) {
        return new MyLearningCourseCard(1L, "트랙", "코스", 0, "다음", lessons,
                PortfolioStatus.LOCKED, List.of(), false, null);
    }

    @Test
    void 진행_중인_수업이_있으면_그_수업으로_보낸다() {
        MyLearningCourseCard card = course(List.of(
                lesson(1, LessonStatus.COMPLETED), lesson(2, LessonStatus.IN_PROGRESS), lesson(3, LessonStatus.LOCKED)));

        assertThat(card.learningLessonId()).isEqualTo(2L);
    }

    @Test
    void 진행_중인_수업이_없으면_열린_수업_중_마지막_수업으로_보낸다() {
        MyLearningCourseCard card = course(List.of(
                lesson(1, LessonStatus.COMPLETED), lesson(2, LessonStatus.COMPLETED)));

        assertThat(card.learningLessonId()).isEqualTo(2L);
    }

    @Test
    void 열린_수업이_없으면_null_이라_화면이_코스_상세로_보낸다() {
        assertThat(course(List.of(lesson(1, LessonStatus.LOCKED))).learningLessonId()).isNull();
        assertThat(course(List.of()).learningLessonId()).isNull();
    }
}
