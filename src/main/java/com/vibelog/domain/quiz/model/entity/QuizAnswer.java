package com.vibelog.domain.quiz.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [QuizAnswer]
 * 역할 : 풀이 1회 안에서 문항 하나에 고른 답 (quiz_answer). 확인 버튼을 누른 시각(checked_at)과 정답 여부를 남긴다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 quiz_answer 그대로 둔다. 풀이 하나에서 같은 문항은 한 번만 답한다 (UNIQUE).
 *        풀이·문항·보기는 ID 로만 가진다. 답 저장 메서드는 퀴즈 풀이·채점(#108)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizAnswerId;

    private Long quizAttemptId;

    private Long quizQuestionId;

    private Long quizChoiceId;

    private boolean isCorrect;

    private LocalDateTime checkedAt;
}
