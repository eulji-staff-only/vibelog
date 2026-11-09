package com.vibelog.domain.course.model.policy;

import com.vibelog.domain.course.model.entity.DiagnosisOption;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 진단 답 검사 (#333): 사용 중인 문항마다 보기를 정확히 하나씩 골라야 한다.
 * 진단 제출(API-DGN-02)과 운영자 테스트 응답(API-ADM-45)이 같이 쓴다. DB 없이 규칙만 본다.
 * 문항 1: 보기 11·12 / 문항 2: 보기 21·22 (사용 중 문항 2개)
 */
class DiagnosisAnswerPolicyTest {

    private final DiagnosisAnswerPolicy policy = new DiagnosisAnswerPolicy();

    private final List<DiagnosisOption> options = List.of(
            DiagnosisOption.create(1L, 100L, "보기 11", 1, 1), DiagnosisOption.create(1L, 200L, "보기 12", 1, 2),
            DiagnosisOption.create(2L, 100L, "보기 21", 1, 1), DiagnosisOption.create(2L, 200L, "보기 22", 1, 2));

    DiagnosisAnswerPolicyTest() {
        // Entity 는 DB 가 ID 를 채우므로 테스트에서만 넣는다
        long[] ids = {11L, 12L, 21L, 22L};
        for (int i = 0; i < ids.length; i++) {
            org.springframework.test.util.ReflectionTestUtils.setField(options.get(i), "diagnosisOptionId", ids[i]);
        }
    }

    @Test
    void 문항마다_하나씩_고르면_고른_순서대로_보기를_돌려준다() {
        assertThat(policy.chosenOptions(2, options, List.of(22L, 11L)))
                .hasValueSatisfying(chosen -> assertThat(chosen).extracting(DiagnosisOption::getContent)
                        .containsExactly("보기 22", "보기 11"));
    }

    @Test
    void 모양이_맞지_않으면_빈_값이다() {
        assertThat(policy.chosenOptions(2, options, List.of(11L))).isEmpty();                   // 답 수가 문항 수보다 적다
        assertThat(policy.chosenOptions(2, options, List.of(11L, 12L))).isEmpty();              // 같은 문항에 두 개
        assertThat(policy.chosenOptions(2, options, List.of(11L, 99L))).isEmpty();              // 사용 중 문항의 보기가 아님
        assertThat(policy.chosenOptions(2, options, Arrays.asList(11L, null))).isEmpty();       // 빈 값
        assertThat(policy.chosenOptions(2, options, null)).isEmpty();
        assertThat(policy.chosenOptions(0, List.of(), List.of())).isEmpty();                    // 사용 중 문항이 없다
    }
}
