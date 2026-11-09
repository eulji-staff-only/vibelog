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
 * [QuizQuestion]
 * 역할 : 퀴즈 문항 테이블(quiz_question)과 매핑된다. 활동(퀴즈) 하나는 문항을 여러 개 가진다.
 * 책임 : 수업 공개 가능 여부 판단(FR-LSN-014, PL-LSN-003, #118)에 필요한 컬럼만 지금 매핑한다
 *        (activityId로 활동별 사용 중인 문항 수를 센다). concept_tag_id·display_type·content·
 *        code_snippet·explanation·sort_order 같은 나머지 컬럼과 등록·수정·사용 중지 도메인 메서드는
 *        퀴즈 문항 등록 기능(#102)에서 채운다. 매핑하지 않은 컬럼이 있어도 Hibernate가 읽기(SELECT)
 *        에는 문제가 없다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizQuestionId;

    private Long activityId;

    private boolean isActive;
}
