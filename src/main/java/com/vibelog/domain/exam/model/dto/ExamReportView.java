package com.vibelog.domain.exam.model.dto;

import java.util.List;

/*
 * [ExamReportView]
 * 역할 : U-16 오답 분석 리포트(API-EXM-05)에 그릴 값.
 * 책임 : - 점수는 정답 수 / 문항 수 (이 회차 기준, PL-EXM-004).
 *        - 개념별 정답률은 정수 %(표시용, 반올림). 약한 부분(60% 미만, PL-LSN-008)은 반올림 전 값으로 판정한다
 *          (예: 25/42 = 59.5% 는 60% 로 보이지만 약한 부분이다).
 *        - nextLessonId 가 null 이면 마지막 수업이라 다음 행동은 포트폴리오다 (계약 ③ ReportNextStep).
 *        - averageScore 는 같은 수업 응시자 평균 점수(1회차, 소수 첫째 자리, P2 FR-EXM-005). 응시자가 5명 미만이면 null 이라 숨긴다.
 */
public record ExamReportView(Long lessonId, Long courseId, String lessonTitle, int attemptNo,
                             int correctCount, int totalCount, List<Concept> concepts, List<Wrong> wrongs,
                             Long nextLessonId, Double averageScore) {

    public static final int WEAK_RATE = 60;   // PL-LSN-008

    public boolean lastLesson() {
        return nextLessonId == null;
    }

    public record Concept(Long activityId, String title, int rate, boolean weak) {

        /* 맞힌 수 ÷ 문항 수로 만든다. rate 는 정수로 반올림, weak 는 반올림 전 비율로 판정 */
        public static Concept of(Long activityId, String title, long correct, int total) {
            return new Concept(activityId, title, (int) Math.round(correct * 100.0 / total), correct * 100 < (long) WEAK_RATE * total);
        }
    }

    /* myChoice 가 null 이면 고르지 않은 문항 */
    public record Wrong(int number, String content, String myChoice, String correctChoice, String explanation,
                        Long conceptActivityId, String conceptTitle) {
    }
}
