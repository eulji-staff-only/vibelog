package com.vibelog.domain.lesson.model.dto;

import java.util.List;

/*
 * 수업 공개 가능 여부와 빠진 항목 (A-06, FR-LSN-014, PL-LSN-003, #118).
 * LessonAdminService.checkPublishStatus가 만든다. missingRequirements는 "개념 뒤 퀴즈", "퀴즈 3~5문항",
 * "정답", "모의고사 문항" 중 빠진 것만 담는다.
 */
public record LessonPublishStatus(boolean publishable, List<String> missingRequirements) {

    public String missingSummary() {
        return String.join(", ", missingRequirements);
    }
}
