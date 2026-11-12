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
    void 약한_개념은_그_태그_정답률이_60퍼센트_미만일_때다() {
        assertThat(policy.isWeak(3, 5)).isFalse();   // 정확히 60% 는 약한 개념이 아니다
        assertThat(policy.isWeak(2, 3)).isFalse();   // 66.67%
        assertThat(policy.isWeak(1, 2)).isTrue();    // 50%
        assertThat(policy.isWeak(7, 12)).isTrue();   // 58.33% — 반올림하면 58% 지만 개수로 비교한다
        assertThat(policy.isWeak(0, 1)).isTrue();
        assertThat(policy.isWeak(1, 1)).isFalse();
    }

    @Test
    void 태그별_오답_비율은_정수로_반올림한다() {
        assertThat(policy.wrongRate(1, 3)).isEqualTo(33);
        assertThat(policy.wrongRate(2, 3)).isEqualTo(67);
        assertThat(policy.wrongRate(1, 2)).isEqualTo(50);
        assertThat(policy.wrongRate(0, 4)).isZero();
    }

    @Test
    void 활동의_통과_기준이_있으면_그_값을_쓴다() {
        assertThat(policy.isPassed(new BigDecimal("66.67"), 70)).isFalse();
        assertThat(policy.isPassed(new BigDecimal("75.00"), 70)).isTrue();
    }
}
