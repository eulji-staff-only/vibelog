package com.vibelog.domain.community.model.dto;

/* 추천·취소 결과 (API-CMT-11 JSON {"recommended", "recommendCount"}). 상세 화면(U-22)도 같은 값을 쓴다 */
public record RecommendResult(boolean recommended, long recommendCount) {
}
