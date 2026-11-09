package com.vibelog.domain.course.model.dto;

/* 카테고리 관리 목록 한 줄 (A-03, API-ADM-06): 이름, 순서, 코스 수(공개·비공개 모두) */
public record AdminCategoryItem(Long courseCategoryId, String name, int sortOrder, long courseCount) {
}
