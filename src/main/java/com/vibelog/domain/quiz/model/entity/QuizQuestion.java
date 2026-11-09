package com.vibelog.domain.quiz.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [QuizQuestion]
 * 역할 : 퀴즈 문항 테이블(quiz_question)과 매핑된다. 활동(퀴즈) 하나는 문항을 여러 개 가진다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 quiz_question 그대로 둔다. 활동·개념 태그는 lesson 영역이라 ID 로만 가진다.
 *        수업 공개 판단(#118)이 activityId·isActive 만 먼저 매핑했고, 퀴즈 풀이(#108)에서 나머지 컬럼을 읽으려고 채웠다.
 *        등록·수정·사용 중지 메서드는 퀴즈 문항 등록(운영자, #102)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizQuestionId;

    private Long activityId;

    private Long conceptTagId;

    @Enumerated(EnumType.STRING)
    private QuizDisplayType displayType;

    private String content;

    private String codeSnippet;     // 코드 빈칸 채우기·결과 유추하기만 있다 (DB CHECK ck_quiz_question_code_snippet)

    private String explanation;

    private int sortOrder;

    private boolean isActive;
}
