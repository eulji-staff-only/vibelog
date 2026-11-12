package com.vibelog.domain.lesson.model.policy;

/*
 * 리포트 확인 뒤 다음 행동 (계약 ③, API-EXM-05).
 * type = NEXT_LESSON·PORTFOLIO. PORTFOLIO면 nextLessonId는 null
 */
public record ReportNextStep(String type, Long courseId, Long nextLessonId) {

    public static final String NEXT_LESSON = "NEXT_LESSON";
    public static final String PORTFOLIO = "PORTFOLIO";
}
