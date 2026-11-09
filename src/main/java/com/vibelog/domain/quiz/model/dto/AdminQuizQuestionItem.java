package com.vibelog.domain.quiz.model.dto;

import com.vibelog.domain.quiz.model.entity.QuizDisplayType;

import java.util.List;

/*
 * [AdminQuizQuestionItem]
 * 역할 : 운영자 수업 편집(A-06) 활동 탭의 퀴즈 문항 한 줄 (QuizReader.getAdminQuestionsOf, #102).
 * 책임 : 사용 중지한 문항도 담는다 (다시 사용할 수 있게). 보기는 순서대로 4개.
 */
public record AdminQuizQuestionItem(Long quizQuestionId, Long activityId, QuizDisplayType displayType, String content,
                                    String codeSnippet, String explanation, Long conceptTagId, int sortOrder,
                                    boolean active, List<Choice> choices) {

    public record Choice(String content, boolean correct, int sortOrder) {
    }
}
