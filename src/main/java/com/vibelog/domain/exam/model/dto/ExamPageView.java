package com.vibelog.domain.exam.model.dto;

import java.util.List;

/*
 * 모의고사 화면(U-15, API-EXM-02)에 그릴 값.
 * 문항 전체와 저장된 내 답(selectedChoiceId, 고르지 않았으면 null)을 같이 담아 이어 풀기를 한다 (PL-EXM-006).
 */
public record ExamPageView(Long lessonId, Long courseId, String lessonTitle,
                           Long examAttemptId, int attemptNo, List<Question> questions) {

    public record Question(Long examQuestionId, String content, List<Choice> choices, Long selectedChoiceId) {
    }

    public record Choice(Long examChoiceId, String content) {
    }
}
