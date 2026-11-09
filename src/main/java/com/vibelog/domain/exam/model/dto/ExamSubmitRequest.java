package com.vibelog.domain.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/*
 * 제출 요청 (API-EXM-04, form).
 * answers 는 마지막 답 상태로, 폼 이름 answers[문항 ID]=보기 ID 로 받는다. 고르지 않은 문항은 빈 값으로 와서 null(선택 취소)이 된다.
 * 폼 값을 Map 으로 묶어 받으려고 record 대신 setter 가 있는 클래스로 둔다 (Entity 가 아니라 @Setter 를 써도 된다).
 */
@Getter
@Setter
public class ExamSubmitRequest {

    @NotNull(message = "응시 정보를 확인해 주세요.")
    private Long examAttemptId;

    private Map<Long, Long> answers = new HashMap<>();
}
