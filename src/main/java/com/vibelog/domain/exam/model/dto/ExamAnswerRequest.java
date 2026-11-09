package com.vibelog.domain.exam.model.dto;

import jakarta.validation.constraints.NotNull;

/* 답 저장 요청 (API-EXM-03, JSON). examChoiceId 를 비우면 선택 취소 */
public record ExamAnswerRequest(@NotNull(message = "문항을 확인해 주세요.") Long examQuestionId,
                                Long examChoiceId) {
}
