package com.vibelog.domain.quiz.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [QuizChoice]
 * 역할 : 퀴즈 문항의 보기 하나 (quiz_choice). 문항 하나에 보기 4개, 그중 정답이 있다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 quiz_choice 그대로 둔다. 순서(sort_order)는 1~4, 문항 안에서 겹치지 않는다.
 *        문항은 ID(quizQuestionId)로만 가진다. 만들기·수정 메서드는 퀴즈 문항 등록(운영자, #102)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizChoice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizChoiceId;

    private Long quizQuestionId;

    private String content;

    private boolean isCorrect;

    private int sortOrder;
}
