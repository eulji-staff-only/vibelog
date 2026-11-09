package com.vibelog.domain.course.model.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/* 카테고리 순서 바꾸기 요청 (API-ADM-10): 모든 카테고리 ID 를 새 순서대로 */
public record CategoryOrderRequest(@NotEmpty List<Long> courseCategoryIds) {
}
