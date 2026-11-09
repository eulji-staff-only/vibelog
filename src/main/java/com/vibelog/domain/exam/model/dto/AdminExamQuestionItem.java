package com.vibelog.domain.exam.model.dto;

import java.util.List;

/*
 * [AdminExamQuestionItem]
 * 역할 : 운영자 수업 편집 화면(A-06)의 모의고사 문항 하나를 나타낸다.
 * 책임 : ExamReader.getQuestionsOf(API-ADM-19, #100)가 돌려주는 읽기 전용 값이다.
 */
public record AdminExamQuestionItem(Long examQuestionId, String content, String explanation,
                                     Long conceptActivityId, int sortOrder, boolean active, List<Choice> choices) {

    public record Choice(String content, boolean correct, int sortOrder) {
    }
}
