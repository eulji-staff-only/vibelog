package com.vibelog.domain.quiz.model.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
 * [QuizGradingPolicy]
 * 역할 : 퀴즈 채점 규칙 (FR-LSN-006, PL-LSN-002, PL-LSN-003).
 * 책임 : - 정답률(%) = 맞힌 수 ÷ 문항 수 × 100, 소수 둘째 자리까지 반올림 (quiz_attempt.correct_rate DECIMAL(5,2)).
 *        - 정답률이 활동의 통과 기준(activity.pass_rate) 이상이면 통과. 통과 기준이 비어 있으면 기본 50.
 *          (3문항이면 2개, 4문항이면 2개, 5문항이면 3개 맞히면 통과, PL-LSN-003)
 *        - DB 를 보지 않는 계산만 한다.
 */
@Component
public class QuizGradingPolicy {

    public static final int DEFAULT_PASS_RATE = 50;

    public BigDecimal correctRate(int correctCount, int questionCount) {
        if (questionCount <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(correctCount * 100L)
                .divide(BigDecimal.valueOf(questionCount), 2, RoundingMode.HALF_UP);
    }

    public boolean isPassed(BigDecimal correctRate, Integer passRate) {
        int threshold = passRate == null ? DEFAULT_PASS_RATE : passRate;
        return correctRate.compareTo(BigDecimal.valueOf(threshold)) >= 0;
    }
}
