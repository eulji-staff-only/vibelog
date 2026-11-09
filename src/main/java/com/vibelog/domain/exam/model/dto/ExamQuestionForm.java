package com.vibelog.domain.exam.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 * [ExamQuestionForm]
 * 역할 : 모의고사 문항 추가·수정 폼 (API-ADM-29·30). 폼 이름 그대로 content, explanation, conceptActivityId,
 *        choices[0]~choices[3], correctIndex 를 받는다.
 * 책임 : - 보기는 4개로 고정한다 (팀 결정, 퀴즈 4지선다 PL-LSN-003 과 같게). correctIndex 는 정답 보기의 순서(0~3).
 *        - 연관 개념이 비면 CONCEPT_REQUIRED 로 따로 알린다 (FR-EXM-007). 그래서 여기서는 @NotNull 을 붙이지 않는다.
 *        - 길이 제한은 sql/01-schema.sql 컬럼 크기 (exam_choice.content VARCHAR(500)).
 *        - 틀리면 A-06 의 같은 모달을 다시 열어 채우려고 setter 가 있는 클래스로 둔다 (Entity 가 아님).
 */
@Getter
@Setter
@NoArgsConstructor
public class ExamQuestionForm {

    public static final int CHOICE_COUNT = 4;

    @NotBlank(message = "문항을 입력해 주세요.")
    private String content;

    @NotBlank(message = "해설을 입력해 주세요.")
    private String explanation;

    private Long conceptActivityId;

    @Size(min = CHOICE_COUNT, max = CHOICE_COUNT, message = "보기 4개를 모두 입력해 주세요.")
    private List<@NotBlank(message = "보기 4개를 모두 입력해 주세요.")
                 @Size(max = 500, message = "보기는 500자까지 입력할 수 있습니다.") String> choices = new ArrayList<>();

    @NotNull(message = "정답 보기를 골라 주세요.")
    @Min(value = 0, message = "정답 보기를 골라 주세요.")
    @Max(value = CHOICE_COUNT - 1, message = "정답 보기를 골라 주세요.")
    private Integer correctIndex;

    /* 화면이 i 번째 보기 칸을 채울 값. 보낸 보기가 모자라면 빈 칸 */
    public String choiceAt(int index) {
        return index < choices.size() ? choices.get(index) : "";
    }
}
