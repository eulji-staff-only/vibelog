package com.vibelog.domain.quiz.model.entity;

/*
 * 퀴즈 문항 표시 유형 (quiz_question.display_type, PL-LSN-003, DB 설계서 4-3).
 * 코드 빈칸 채우기 / 개념 문제 / 결과 유추하기 / 낚시 문제. 모두 객관식 4지선다다.
 */
public enum QuizDisplayType {
    CODE_BLANK("코드 빈칸 채우기", true),
    CONCEPT("개념 문제", false),
    OUTPUT_PREDICT("결과 유추하기", true),
    TRICK("낚시 문제", false);

    private final String label;
    private final boolean codeRequired;

    QuizDisplayType(String label, boolean codeRequired) {
        this.label = label;
        this.codeRequired = codeRequired;
    }

    /* 운영자 화면(A-06)에 보여 줄 이름 (FR-LSN-012) */
    public String getLabel() {
        return label;
    }

    /* 코드 빈칸 채우기·결과 유추하기는 코드가 꼭 있어야 한다 (FR-LSN-012, DB ck_quiz_question_code_snippet) */
    public boolean isCodeRequired() {
        return codeRequired;
    }
}
