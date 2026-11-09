package com.vibelog.domain.quiz.model.dto;

import com.vibelog.domain.quiz.model.entity.QuizDisplayType;
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
 * [QuizQuestionForm]
 * 역할 : 퀴즈 문항 추가·수정 폼 (API-ADM-26·27, FR-LSN-012, #102).
 * 책임 : - 보기 4개·정답 1개 (correctIndex 0~3). 보기 길이는 sql/01-schema.sql quiz_choice.content(500자) 그대로.
 *        - 코드 빈칸 채우기·결과 유추하기는 코드가 필수다 (needsCode). 검사는 컨트롤러가 칸 메시지로 모은다.
 *        - 폼 바인딩에 쓰여 Setter 가 있다 (Entity 가 아니라 입력 DTO). 모의고사 문항 폼(ExamQuestionForm)과 같은 방식.
 */
@Getter
@Setter
@NoArgsConstructor
public class QuizQuestionForm {

    public static final int CHOICE_COUNT = 4;

    @NotNull(message = "표시 유형을 골라 주세요.")
    private QuizDisplayType displayType;

    @NotBlank(message = "문항을 입력해 주세요.")
    private String content;

    private String codeSnippet;

    @NotBlank(message = "해설을 입력해 주세요.")
    private String explanation;

    @NotNull(message = "개념 태그를 골라 주세요.")
    private Long conceptTagId;

    @Size(min = CHOICE_COUNT, max = CHOICE_COUNT, message = "보기 4개를 모두 입력해 주세요.")
    private List<@NotBlank(message = "보기 4개를 모두 입력해 주세요.")
                 @Size(max = 500, message = "보기는 500자까지 입력할 수 있습니다.") String> choices = new ArrayList<>();

    @NotNull(message = "정답 보기를 골라 주세요.")
    @Min(value = 0, message = "정답 보기를 골라 주세요.")
    @Max(value = CHOICE_COUNT - 1, message = "정답 보기를 골라 주세요.")
    private Integer correctIndex;

    /* 코드가 필요한 유형인데 코드가 비었는가 (FR-LSN-012, DB ck_quiz_question_code_snippet) */
    public boolean needsCode() {
        return displayType != null && displayType.isCodeRequired() && (codeSnippet == null || codeSnippet.isBlank());
    }

    /* 저장할 코드. 코드가 필요 없는 유형이거나 비었으면 null */
    public String codeToSave() {
        return codeSnippet == null || codeSnippet.isBlank() ? null : codeSnippet;
    }

    /* 화면이 i 번째 보기 칸을 채울 값. 보낸 보기가 모자라면 빈 칸 */
    public String choiceAt(int index) {
        return index < choices.size() ? choices.get(index) : "";
    }

    public static QuizQuestionForm of(QuizDisplayType displayType, String content, String codeSnippet, String explanation,
                                      Long conceptTagId, List<String> choices, Integer correctIndex) {
        QuizQuestionForm form = new QuizQuestionForm();
        form.displayType = displayType;
        form.content = content;
        form.codeSnippet = codeSnippet;
        form.explanation = explanation;
        form.conceptTagId = conceptTagId;
        form.choices = new ArrayList<>(choices);
        form.correctIndex = correctIndex;
        return form;
    }
}
