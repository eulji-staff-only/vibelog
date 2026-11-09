package com.vibelog.domain.course.model.dto;

import java.util.List;

/*
 * 진단 설문 화면(U-07, API-DGN-01) 데이터: 사용 중인 문항 전체와 보기. 문항 전환은 화면 안 JS 가 한다.
 * 보기의 트랙·점수는 화면에 보내지 않는다 (추천 계산은 서버에서만, PL-DGN-002).
 */
public record DiagnosisSurveyView(List<Question> questions) {

    public record Question(Long diagnosisQuestionId, String content, List<Option> options) {
    }

    public record Option(Long diagnosisOptionId, String content) {
    }

    public int total() {
        return questions.size();
    }
}
