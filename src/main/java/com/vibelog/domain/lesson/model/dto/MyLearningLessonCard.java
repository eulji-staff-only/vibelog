package com.vibelog.domain.lesson.model.dto;

/*
 * [MyLearningLessonCard]
 * 역할 : 내 학습(U-12, API-ENR-02) 코스 카드 안 수업 한 줄의 상태값.
 *        reportUrl 은 제출한 모의고사 회차가 있을 때만 있는 "리포트 보기" 링크(FR-EXM-006), 없으면 null.
 */
public record MyLearningLessonCard(
        Long lessonId,
        int order,
        String title,
        LessonStatus status,
        LessonExamStatus examStatus,
        String reportUrl
) {
    /* 리포트 링크가 없는 한 줄 */
    public MyLearningLessonCard(Long lessonId, int order, String title, LessonStatus status, LessonExamStatus examStatus) {
        this(lessonId, order, title, status, examStatus, null);
    }

    public boolean hasReport() {
        return reportUrl != null;
    }
}
