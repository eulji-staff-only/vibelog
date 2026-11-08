package com.vibelog.domain.lesson.model.dto;

/*
 * 수업 화면 GET(API-LSN-01)의 처리 결과.
 * NOT_ENROLLED·LOCKED 는 에러 화면이 아니라 "다른 화면으로 이동 + 토스트"(_template.md 0-1 ③)라서
 * Controller 가 바로 redirect 로 돌려준다 (data 는 OK 일 때만 채운다).
 */
public record LessonPageResult(Status status, LessonPageData data) {

    public enum Status { NOT_ENROLLED, LOCKED, OK }

    public static LessonPageResult notEnrolled() {
        return new LessonPageResult(Status.NOT_ENROLLED, null);
    }

    public static LessonPageResult locked() {
        return new LessonPageResult(Status.LOCKED, null);
    }

    public static LessonPageResult ok(LessonPageData data) {
        return new LessonPageResult(Status.OK, data);
    }
}
