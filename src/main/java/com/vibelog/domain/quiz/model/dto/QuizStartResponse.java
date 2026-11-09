package com.vibelog.domain.quiz.model.dto;

import java.util.List;

/*
 * 풀이 시작 응답 (API-LSN-04). 정답·해설은 넣지 않는다 (문항 확인 API-LSN-05 에서만 준다).
 * 처음 풀면 문항·보기를 정해진 순서대로, 다시 풀면 섞어서 준다 (PL-LSN-004·005).
 */
public record QuizStartResponse(Long quizAttemptId, List<Question> questions) {

    public record Question(Long quizQuestionId, String displayType, String content, String codeSnippet,
                           List<Choice> choices) {
    }

    public record Choice(Long quizChoiceId, String content) {
    }
}
