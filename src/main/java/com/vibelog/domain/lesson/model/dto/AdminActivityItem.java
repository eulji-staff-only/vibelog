package com.vibelog.domain.lesson.model.dto;

import com.vibelog.domain.lesson.model.entity.ActivityType;

import java.util.List;

/*
 * [AdminActivityItem]
 * 역할 : 운영자 수업 편집 화면(A-06)의 활동 하나를 나타낸다.
 * 책임 : API-ADM-19 목록(#100)과 API-ADM-22 수정 폼에 채울 값(#101)을 함께 담는다.
 *        body·toolNoteUrl·estimatedMinutes·tagNames는 "활동 수정" 폼(이 활동으로 th:object)을 채우는 값이다.
 */
public record AdminActivityItem(Long activityId, ActivityType activityType, String title, String body,
                                 String toolNoteUrl, Integer estimatedMinutes, List<String> tagNames,
                                 int sortOrder, boolean active) {

    /* 수정 폼의 "개념 태그" 입력칸(쉼표 구분 하나)에 채울 값 */
    public String tagNamesJoined() {
        return String.join(", ", tagNames);
    }
}
