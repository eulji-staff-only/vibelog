package com.vibelog.domain.exam.model.dto;

/*
 * 응시 시작·이어 풀기(API-EXM-01) 결과.
 * LOCKED·REPORT_NOT_VIEWED 는 에러 화면이 아니라 "다른 화면 + 토스트"(_template.md 7-1 ③)라서
 * 예외 대신 결과로 돌려주고 Controller 가 redirect 한다. attemptNo 는 REPORT_NOT_VIEWED 일 때 리포트를 볼 회차.
 */
public record ExamStartResult(Status status, Integer attemptNo) {

    public enum Status { OK, LOCKED, REPORT_NOT_VIEWED }

    public static ExamStartResult ok() {
        return new ExamStartResult(Status.OK, null);
    }

    public static ExamStartResult locked() {
        return new ExamStartResult(Status.LOCKED, null);
    }

    public static ExamStartResult reportNotViewed(int attemptNo) {
        return new ExamStartResult(Status.REPORT_NOT_VIEWED, attemptNo);
    }
}
