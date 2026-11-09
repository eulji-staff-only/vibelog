package com.vibelog.domain.quiz.model.dto;

import jakarta.validation.constraints.NotNull;

/* 문항 확인 요청 (API-LSN-05): 어느 문항에서 어느 보기를 골랐는지 */
public record QuizCheckRequest(@NotNull Long quizQuestionId, @NotNull Long quizChoiceId) {
}
