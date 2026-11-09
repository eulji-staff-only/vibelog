package com.vibelog.domain.portfolio.model.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* API 키 형식 가리기 (PL-PRT-008): 정책의 접두어 5개만 가리고, 단어 중간은 건드리지 않는다 */
class ApiKeyMaskerTest {

    @Test
    void 정책의_접두어로_시작하는_문자열을_가린다() {
        assertThat(ApiKeyMasker.mask("sk-abc123 ghp_XYZ github_pat_11AA AKIAIOSFODNN7 AIzaSyD-x_1"))
                .isEqualTo("sk-**** ghp_**** github_pat_**** AKIA**** AIza****");
    }

    @Test
    void 단어_중간에_있는_것과_빈_값은_그대로_둔다() {
        assertThat(ApiKeyMasker.mask("task-1, disk-full")).isEqualTo("task-1, disk-full");
        assertThat(ApiKeyMasker.mask(null)).isNull();
    }
}
