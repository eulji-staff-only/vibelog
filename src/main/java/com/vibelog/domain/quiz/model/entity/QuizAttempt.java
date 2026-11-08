package com.vibelog.domain.quiz.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * [QuizAttempt]
 * 역할 : 회원의 퀴즈 풀이 1회 (quiz_attempt). 퀴즈는 lesson 영역의 활동(activity)이라 ID 로만 가진다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 quiz_attempt 그대로 둔다.
 *        제출 전에는 correct_rate·is_passed·submitted_at 이 비어 있다 (DB CHECK ck_quiz_attempt_submitted).
 *        운영자 회원 상세(#119)가 퀴즈 결과를 읽으려고 먼저 만들었다. quiz 기반(#84)에서 나머지 Entity 와
 *        풀이 시작·제출 메서드는 퀴즈 풀이(#108)처럼 쓰는 곳이 생길 때 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttempt extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizAttemptId;

    private Long memberId;

    private Long activityId;

    private LocalDateTime startedAt;

    private BigDecimal correctRate;     // 정답률(%), 제출 전 NULL

    private Boolean isPassed;           // 50% 이상이면 통과 (PL-LSN-002), 제출 전 NULL

    private LocalDateTime submittedAt;
}
