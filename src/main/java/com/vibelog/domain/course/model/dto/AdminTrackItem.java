package com.vibelog.domain.course.model.dto;

/* 트랙 관리 목록 한 줄 (A-02, API-ADM-02): 이름, 대상, 설명, 순서, 코스 수(공개·비공개 모두) */
public record AdminTrackItem(Long trackId, String name, String target, String description, int sortOrder, long courseCount) {
}
