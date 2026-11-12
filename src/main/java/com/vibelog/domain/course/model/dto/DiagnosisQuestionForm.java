package com.vibelog.domain.course.model.dto;

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
 * [DiagnosisQuestionForm]
 * 역할 : 진단 문항 추가·수정 폼 (API-ADM-42·43, FR-DGN-004, #187).
 * 책임 : - 길이는 sql/01-schema.sql 그대로 (문항 500자, 보기 255자). 보기는 2개 이상, 점수는 1 이상 INT 범위 (admin.md 결정 1).
 *        - 화면은 보기 칸을 넉넉히 그리고, 아무것도 입력하지 않은 줄은 보기로 치지 않는다 (filledOptions).
 *        - 폼 바인딩(th:field)에 쓰여 Setter 가 있다 (Entity 가 아니라 입력 DTO).
 */
@Getter
@Setter
@NoArgsConstructor
public class DiagnosisQuestionForm {

    public static final int MIN_OPTIONS = 2;
    public static final int CONTENT_MAX = 500;
    public static final int OPTION_CONTENT_MAX = 255;
    /* 화면이 그리는 보기 칸 수의 최소값 */
    public static final int OPTION_ROWS = 4;

    @NotBlank(message = "문항을 입력해 주세요.")
    @Size(max = CONTENT_MAX, message = "문항은 500자까지 쓸 수 있습니다.")
    private String content;

    @NotNull(message = "순서를 입력해 주세요.")
    @Min(value = 1, message = "순서는 1 이상이어야 합니다.")
    private Integer sortOrder;

    private List<DiagnosisOptionForm> options = new ArrayList<>();

    /* 빈 줄을 뺀 보기 */
    public List<DiagnosisOptionForm> filledOptions() {
        return options.stream().filter(option -> !option.isBlank()).toList();
    }

    /* 보기 칸 규칙을 어긴 첫 문구. 맞으면 null (점수가 INT 범위를 넘으면 바인딩 단계에서 따로 막힌다) */
    public String optionError() {
        List<DiagnosisOptionForm> filled = filledOptions();
        for (DiagnosisOptionForm option : filled) {
            if (option.getContent() == null || option.getContent().isBlank()) {
                return "보기 내용을 입력해 주세요.";
            }
            if (option.getContent().length() > OPTION_CONTENT_MAX) {
                return "보기는 255자까지 쓸 수 있습니다.";
            }
            if (option.getTrackId() == null) {
                return "보기마다 트랙을 골라 주세요.";
            }
            if (option.getScore() == null || option.getScore() < 1) {
                return "점수는 1 이상이어야 합니다.";
            }
        }
        if (filled.size() < MIN_OPTIONS) {
            return "보기를 2개 이상 입력해 주세요.";
        }
        return null;
    }

    /* 화면이 보기 칸을 OPTION_ROWS 이상, 그리고 빈 칸이 하나는 남게 그리도록 채운다 */
    public DiagnosisQuestionForm withBlankRows() {
        int rows = Math.max(OPTION_ROWS, options.size() + 1);
        while (options.size() < rows) {
            options.add(new DiagnosisOptionForm());
        }
        return this;
    }

    public static DiagnosisQuestionForm of(String content, Integer sortOrder, List<DiagnosisOptionForm> options) {
        DiagnosisQuestionForm form = new DiagnosisQuestionForm();
        form.content = content;
        form.sortOrder = sortOrder;
        form.options = new ArrayList<>(options);
        return form;
    }
}
