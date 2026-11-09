package com.vibelog.domain.quiz.model.entity;

/*
 * 퀴즈 문항 표시 유형 (quiz_question.display_type, PL-LSN-003, DB 설계서 4-3).
 * 코드 빈칸 채우기 / 개념 문제 / 결과 유추하기 / 낚시 문제. 모두 객관식 4지선다다.
 */
public enum QuizDisplayType {
    CODE_BLANK,
    CONCEPT,
    OUTPUT_PREDICT,
    TRICK
}
