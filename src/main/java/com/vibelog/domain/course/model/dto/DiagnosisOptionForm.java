package com.vibelog.domain.course.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 진단 문항 폼의 보기 한 줄 (API-ADM-42·43, #187). diagnosisOptionId 는 수정할 때 이미 있는 보기만 채운다.
 * 검사는 DiagnosisQuestionForm.optionError 가 한 번에 한다 (빈 줄은 보기로 치지 않는다).
 */
@Getter
@Setter
@NoArgsConstructor
public class DiagnosisOptionForm {

    private Long diagnosisOptionId;

    private String content;

    private Long trackId;

    private Integer score;

    public static DiagnosisOptionForm of(Long diagnosisOptionId, String content, Long trackId, Integer score) {
        DiagnosisOptionForm form = new DiagnosisOptionForm();
        form.diagnosisOptionId = diagnosisOptionId;
        form.content = content;
        form.trackId = trackId;
        form.score = score;
        return form;
    }

    /* 아무것도 입력하지 않은 줄 (보기로 치지 않는다) */
    public boolean isBlank() {
        return (content == null || content.isBlank()) && trackId == null && score == null;
    }
}
