package com.vibelog.domain.lesson.model.policy;

/*
 * 퀴즈 통과 뒤 다음 행동 (계약 ①, API-LSN-06 "next").
 * type = CONCEPT·EXAM·NONE. EXAM·NONE이면 activityId는 null
 */
public record NextStep(String type, Long courseId, Long lessonId, Long activityId) {

    public static final String CONCEPT = "CONCEPT";
    public static final String EXAM = "EXAM";
    public static final String NONE = "NONE";
}
