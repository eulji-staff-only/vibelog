package com.vibelog.domain.exam.model.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/* 모의고사 응시자 통계 규칙 (PL-EXM-004·008): 5명 미만이면 보여주지 않고, 평균은 소수 첫째 자리 */
class ExamStatPolicyTest {

    @Test
    void 응시자가_5명_미만이면_평균을_만들지_않는다() {
        assertThat(ExamStatPolicy.average(List.of(100, 90, 80, 70))).isNull();
        assertThat(ExamStatPolicy.hasEnoughRespondents(4)).isFalse();
    }

    @Test
    void 응시자가_5명이면_평균을_낸다() {
        assertThat(ExamStatPolicy.average(List.of(100, 80, 60, 40, 21))).isEqualTo(60.2);
        assertThat(ExamStatPolicy.hasEnoughRespondents(5)).isTrue();
    }

    @Test
    void 평균은_소수_첫째_자리로_반올림한다() {
        assertThat(ExamStatPolicy.average(List.of(100, 100, 100, 100, 100, 99))).isEqualTo(99.8);   // 99.833…
    }

    /* DB 에서 합계·인원수만 센 경우(#481)도 목록 버전과 같은 기준·같은 값이어야 한다 */
    @Test
    void 합계와_인원수로_낸_평균은_목록으로_낸_평균과_같다() {
        assertThat(ExamStatPolicy.average(4, 4)).isNull();                                          // 5명 미만
        assertThat(ExamStatPolicy.average(301, 5)).isEqualTo(ExamStatPolicy.average(List.of(100, 80, 60, 40, 21)));   // 60.2
        assertThat(ExamStatPolicy.average(599, 6)).isEqualTo(99.8);                                 // 99.833…
        assertThat(ExamStatPolicy.average(0, 5)).isEqualTo(0.0);                                    // 모두 0점
    }
}
