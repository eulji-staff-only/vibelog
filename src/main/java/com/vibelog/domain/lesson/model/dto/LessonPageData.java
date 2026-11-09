package com.vibelog.domain.lesson.model.dto;

import java.util.List;

/* 수업 화면(U-13)에 보여줄 데이터 (API-LSN-01). preview 는 운영자 미리보기(FR-LSN-015)라서 기록 버튼을 숨긴다 */
public record LessonPageData(
        Long courseId,
        Long lessonId,
        String lessonTitle,
        String lessonGoal,
        List<LessonActivityCard> cards,
        boolean preview
) {
    /* 학습자 화면(미리보기 아님) */
    public LessonPageData(Long courseId, Long lessonId, String lessonTitle, String lessonGoal,
                          List<LessonActivityCard> cards) {
        this(courseId, lessonId, lessonTitle, lessonGoal, cards, false);
    }
}
