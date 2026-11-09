package com.vibelog.domain.exam.model.dto;

import java.util.List;

/* 수업 편집 화면(API-ADM-19, #100)의 모의고사 문항 하나. ExamReader.getQuestionsOf 가 돌려준다 */
public record AdminExamQuestionItem(Long examQuestionId, String content, String explanation,
                                     Long conceptActivityId, int sortOrder, boolean active, List<Choice> choices) {

    public record Choice(String content, boolean correct, int sortOrder) {
    }
}
