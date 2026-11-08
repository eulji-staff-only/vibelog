package com.vibelog.domain.lesson.model.dto;

/*
 * [LessonExamStatus]
 * 역할 : 내 학습(U-12, API-ENR-02) 수업별 모의고사 상태.
 * 책임 : 수업이 잠겨 있으면 모의고사도 잠김. 수업이 완료(리포트 확인까지 끝남)면 리포트 확인.
 *        그 외 열린 수업은 계약 ④(LessonExamStatusChecker)에 마지막 제출 응시가 있으면 응시함, 없으면 열림(응시 안 함)이다.
 */
public enum LessonExamStatus {

    LOCKED("잠김", "ml-badge-locked"),
    OPEN("열림", "ml-badge-strong"),
    ATTEMPTED("응시함", "ml-badge-strong"),
    REPORT_CONFIRMED("리포트 확인", "ml-badge-success");

    private final String label;
    private final String badgeClass;

    LessonExamStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String label() {
        return label;
    }

    public String badgeClass() {
        return badgeClass;
    }
}
