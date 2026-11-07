package com.vibelog.domain.lesson.model.dto;

/*
 * 개념 읽기 완료 POST(API-LSN-02)의 처리 결과.
 * locked 면 에러 화면이 아니라 "다른 화면으로 이동 + 토스트"(_template.md 0-1 ③, ACTIVITY_LOCKED → 수업 화면)라서
 * Controller 가 바로 redirect 로 돌려준다.
 */
public record CompleteActivityResult(boolean locked, String redirectUrl) {

    public static CompleteActivityResult ofLocked() {
        return new CompleteActivityResult(true, null);
    }

    public static CompleteActivityResult redirectTo(String url) {
        return new CompleteActivityResult(false, url);
    }
}
