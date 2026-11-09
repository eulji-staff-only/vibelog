package com.vibelog.domain.portfolio.model.dto;

import jakarta.validation.constraints.NotNull;

/* 스텝 체크 요청 (API-PRT-02, JSON). true 면 체크 저장, false 면 체크 삭제 */
public record GuideStepCheckRequest(@NotNull(message = "체크 여부를 확인해 주세요.") Boolean checked) {
}
