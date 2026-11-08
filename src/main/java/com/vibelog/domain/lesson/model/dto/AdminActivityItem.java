package com.vibelog.domain.lesson.model.dto;

import com.vibelog.domain.lesson.model.entity.ActivityType;

/* 수업 편집 화면(A-06, API-ADM-19, #100)의 활동 하나. 추가·수정·순서 변경은 활동 관리 이슈(#101)에서 만든다 */
public record AdminActivityItem(Long activityId, ActivityType activityType, String title, int sortOrder, boolean active) {
}
