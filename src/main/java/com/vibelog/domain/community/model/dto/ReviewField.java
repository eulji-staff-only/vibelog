package com.vibelog.domain.community.model.dto;

/* 리뷰 폼의 칸 하나: 평가 기준(없으면 null)과 지금 내용 (U-31) */
public record ReviewField(Long reviewCriterionId, String name, String description, String content) {
}
