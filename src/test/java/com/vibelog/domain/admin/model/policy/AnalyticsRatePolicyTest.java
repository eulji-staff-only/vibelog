package com.vibelog.domain.admin.model.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* 확장 대시보드(FR-ADM-003) 2차 지표의 비율·약한 개념 규칙 (PL-LSN-008) */
class AnalyticsRatePolicyTest {

    @Test
    void 비율은_분자_나누기_분모를_정수로_반올림하고_분모가_0이면_0이다() {
        assertThat(AnalyticsRatePolicy.percent(1, 3)).isEqualTo(33);
        assertThat(AnalyticsRatePolicy.percent(2, 3)).isEqualTo(67);
        assertThat(AnalyticsRatePolicy.percent(3, 3)).isEqualTo(100);
        assertThat(AnalyticsRatePolicy.percent(0, 0)).isZero();
    }

    @Test
    void 약한_개념은_정답률_60퍼센트_미만이고_정확히_60퍼센트는_약하지_않다() {
        assertThat(AnalyticsRatePolicy.isWeakConcept(4, 10)).isFalse();   // 정답률 60%
        assertThat(AnalyticsRatePolicy.isWeakConcept(5, 10)).isTrue();    // 정답률 50%
        assertThat(AnalyticsRatePolicy.isWeakConcept(2, 5)).isFalse();    // 정답률 60%
        assertThat(AnalyticsRatePolicy.isWeakConcept(3, 5)).isTrue();     // 정답률 40%
    }

    @Test
    void 반올림하면_60퍼센트로_보이는_59점5퍼센트도_약한_개념이다() {
        assertThat(AnalyticsRatePolicy.isWeakConcept(17, 42)).isTrue();   // 25/42 = 59.5%
        assertThat(AnalyticsRatePolicy.isWeakConcept(0, 0)).isFalse();
    }
}
