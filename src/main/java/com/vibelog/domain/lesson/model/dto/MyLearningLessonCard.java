package com.vibelog.domain.lesson.model.dto;

/*
 * [MyLearningLessonCard]
 * 역할 : 내 학습(U-12, API-ENR-02) 코스 카드 안 수업 한 줄의 상태값.
 */
public record MyLearningLessonCard(
        Long lessonId,
        int order,
        String title,
        LessonStatus status,
        LessonExamStatus examStatus
) {
}
