package com.vibelog.domain.exam.model.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* 약한 부분(60% 미만, PL-LSN-008)은 반올림 전 비율로 판정한다 */
class ExamReportViewTest {

    @Test
    void 반올림하면_60퍼센트여도_실제로_60퍼센트_미만이면_약한_부분이다() {
        ExamReportView.Concept concept = ExamReportView.Concept.of(1L, "개념", 25, 42);   // 59.52%
        assertThat(concept.rate()).isEqualTo(60);
        assertThat(concept.weak()).isTrue();
    }

    @Test
    void 딱_60퍼센트는_약한_부분이_아니다() {
        ExamReportView.Concept concept = ExamReportView.Concept.of(1L, "개념", 3, 5);
        assertThat(concept.rate()).isEqualTo(60);
        assertThat(concept.weak()).isFalse();
    }
}
