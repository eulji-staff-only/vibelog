package com.vibelog.domain.portfolio.model.dto;

/* 스텝 체크 응답 (API-PRT-02, JSON). 화면의 "n / 전체 완료"에 쓴다 */
public record GuideStepCheckResult(long checkedCount, long totalCount) {
}
