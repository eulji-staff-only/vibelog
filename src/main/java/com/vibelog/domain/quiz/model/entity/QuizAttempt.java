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
 *        풀이 시작은 create, 제출·채점 결과 기록은 submit 으로만 한다 (#108). 한 번 제출한 풀이는 다시 제출할 수 없다.
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

    private Boolean isPassed;           // 통과 기준(activity.pass_rate, 기본 50) 이상이면 통과 (PL-LSN-002), 제출 전 NULL

    private LocalDateTime submittedAt;

    private QuizAttempt(Long memberId, Long activityId, LocalDateTime startedAt) {
        this.memberId = memberId;
        this.activityId = activityId;
        this.startedAt = startedAt;
    }

    /* 풀이 시작 (API-LSN-04) */
    public static QuizAttempt create(Long memberId, Long activityId, LocalDateTime startedAt) {
        return new QuizAttempt(memberId, activityId, startedAt);
    }

    /* 제출·채점 결과 기록 (API-LSN-06). 정답률·통과 여부·제출 시각을 함께 채운다 (DB CHECK ck_quiz_attempt_submitted) */
    public void submit(BigDecimal correctRate, boolean passed, LocalDateTime submittedAt) {
        this.correctRate = correctRate;
        this.isPassed = passed;
        this.submittedAt = submittedAt;
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
