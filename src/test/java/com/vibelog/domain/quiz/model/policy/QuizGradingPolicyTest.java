package com.vibelog.domain.quiz.model.policy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/* 퀴즈 채점 규칙 (PL-LSN-002, PL-LSN-003): 정답률 = 맞힌 수 ÷ 문항 수, 통과 기준(기본 50%) 이상이면 통과 */
class QuizGradingPolicyTest {

    private final QuizGradingPolicy policy = new QuizGradingPolicy();

    @Test
    void 정답률은_소수_둘째_자리까지_반올림한다() {
        assertThat(policy.correctRate(3, 4)).isEqualByComparingTo("75.00");
        assertThat(policy.correctRate(2, 3)).isEqualByComparingTo("66.67");
        assertThat(policy.correctRate(0, 5)).isEqualByComparingTo("0.00");
    }

    @Test
    void 기본_통과_기준은_50퍼센트이고_PL_LSN_003_의_문항수별_통과_개수와_맞는다() {
        // 3문항이면 2개, 4문항이면 2개, 5문항이면 3개 맞혀야 통과
        assertThat(policy.isPassed(policy.correctRate(1, 3), null)).isFalse();
        assertThat(policy.isPassed(policy.correctRate(2, 3), null)).isTrue();
        assertThat(policy.isPassed(policy.correctRate(1, 4), null)).isFalse();
        assertThat(policy.isPassed(policy.correctRate(2, 4), null)).isTrue();   // 정확히 50% 는 통과
        assertThat(policy.isPassed(policy.correctRate(2, 5), null)).isFalse();
        assertThat(policy.isPassed(policy.correctRate(3, 5), null)).isTrue();
    }

    @Test
    void 활동의_통과_기준이_있으면_그_값을_쓴다() {
        assertThat(policy.isPassed(new BigDecimal("66.67"), 70)).isFalse();
        assertThat(policy.isPassed(new BigDecimal("75.00"), 70)).isTrue();
    }
}
