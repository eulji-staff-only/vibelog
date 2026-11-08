package com.vibelog.domain.lesson.model.dto;

import java.util.List;

/* 수업 화면(U-13)에 보여줄 데이터 (API-LSN-01) */
public record LessonPageData(
        Long courseId,
        Long lessonId,
        String lessonTitle,
        String lessonGoal,
        List<LessonActivityCard> cards
) {
}
