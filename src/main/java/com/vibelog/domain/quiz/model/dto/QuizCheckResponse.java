package com.vibelog.domain.quiz.model.dto;

/* 문항 확인 응답 (API-LSN-05): 맞았는지, 정답 보기, 해설. 이 뒤로 이 문항의 답은 바꿀 수 없다 (PL-LSN-006) */
public record QuizCheckResponse(boolean correct, Long correctQuizChoiceId, String explanation) {
}
