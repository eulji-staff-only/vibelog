package com.vibelog.domain.admin.model.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* 진단 완료·건너뛰기 비율 (A-01 확장 대시보드, FR-ADM-003) */
class DiagnosisRatioPolicyTest {

    @Test
    void 결과가_없으면_둘_다_0퍼센트다() {
        assertThat(DiagnosisRatioPolicy.completedRate(0, 0)).isZero();
        assertThat(DiagnosisRatioPolicy.skippedRate(0, 0)).isZero();
    }

    @Test
    void 완료_4건_건너뜀_1건이면_80과_20이다() {
        assertThat(DiagnosisRatioPolicy.completedRate(4, 1)).isEqualTo(80);
        assertThat(DiagnosisRatioPolicy.skippedRate(4, 1)).isEqualTo(20);
    }

    @Test
    void 반올림해도_두_비율의_합은_100이다() {
        // 1/8 = 12.5%, 7/8 = 87.5% 를 각각 반올림하면 13 + 88 = 101 이 되므로 건너뜀은 100 에서 뺀다
        assertThat(DiagnosisRatioPolicy.completedRate(7, 1)).isEqualTo(88);
        assertThat(DiagnosisRatioPolicy.skippedRate(7, 1)).isEqualTo(12);
    }

    @Test
    void 전부_건너뛰면_건너뜀_100퍼센트다() {
        assertThat(DiagnosisRatioPolicy.completedRate(0, 3)).isZero();
        assertThat(DiagnosisRatioPolicy.skippedRate(0, 3)).isEqualTo(100);
    }
}
