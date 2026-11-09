package com.vibelog.domain.admin.model.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 완료율 계산 규칙 (FR-ADM-002): 분모는 그 기간에 시작한 수강 수, 분자는 그중 완료한 수강 수다.
 * 시작한 수강이 0건이면 0%(나누기 0 방지), 나머지는 정수 %로 반올림한다.
 */
class CompletionRatePolicyTest {

    @Test
    void 시작한_수강이_없으면_0퍼센트다() {
        assertThat(CompletionRatePolicy.calculate(0, 0)).isEqualTo(0);
    }

    @Test
    void 모두_완료하면_100퍼센트다() {
        assertThat(CompletionRatePolicy.calculate(5, 5)).isEqualTo(100);
    }

    @Test
    void 나누어떨어지지_않으면_반올림한다() {
        // 2/3 = 66.666... -> 67
        assertThat(CompletionRatePolicy.calculate(3, 2)).isEqualTo(67);
        // 1/3 = 33.333... -> 33
        assertThat(CompletionRatePolicy.calculate(3, 1)).isEqualTo(33);
    }

    @Test
    void 완료한_수강이_없으면_0퍼센트다() {
        assertThat(CompletionRatePolicy.calculate(4, 0)).isEqualTo(0);
    }
}
