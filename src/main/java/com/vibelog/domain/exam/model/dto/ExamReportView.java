package com.vibelog.domain.exam.model.dto;

import java.util.List;

/*
 * [ExamReportView]
 * 역할 : U-16 오답 분석 리포트(API-EXM-05)에 그릴 값.
 * 책임 : - 점수는 정답 수 / 문항 수 (이 회차 기준, PL-EXM-004).
 *        - 개념별 정답률은 정수 %, 60% 미만이면 약한 부분 (PL-LSN-008).
 *        - nextLessonId 가 null 이면 마지막 수업이라 다음 행동은 포트폴리오다 (계약 ③ ReportNextStep).
 */
public record ExamReportView(Long lessonId, Long courseId, String lessonTitle, int attemptNo,
                             int correctCount, int totalCount, List<Concept> concepts, List<Wrong> wrongs,
                             Long nextLessonId) {

    public static final int WEAK_RATE = 60;   // PL-LSN-008

    public boolean lastLesson() {
        return nextLessonId == null;
    }

    public record Concept(Long activityId, String title, int rate) {
        public boolean weak() {
            return rate < WEAK_RATE;
        }
    }

    /* myChoice 가 null 이면 고르지 않은 문항 */
    public record Wrong(int number, String content, String myChoice, String correctChoice, String explanation,
                        Long conceptActivityId, String conceptTitle) {
    }
}
