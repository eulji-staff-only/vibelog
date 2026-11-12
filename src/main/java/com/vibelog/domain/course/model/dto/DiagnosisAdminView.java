package com.vibelog.domain.course.model.dto;

import java.util.List;

/*
 * 진단 설정 화면(A-09, API-ADM-41, #187) 데이터: 문항 전체(사용 중지 포함, 순서대로)와 보기, 트랙 목록.
 * 테스트 응답 칸은 사용 중인 문항만 쓴다 (activeQuestions).
 */
public record DiagnosisAdminView(List<Question> questions, List<CourseOption> tracks) {

    public List<Question> activeQuestions() {
        return questions.stream().filter(Question::active).toList();
    }

    public record Question(Long diagnosisQuestionId, String content, int sortOrder, boolean active, List<Option> options) {
    }

    public record Option(Long diagnosisOptionId, String content, Long trackId, String trackName, int score) {
    }
}
