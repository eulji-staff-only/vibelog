package com.vibelog.domain.auth.model.policy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 5회 연속 실패하면 5분 잠금 (PL-USR-007). 시간을 직접 움직여서 확인한다.
 */
class LoginLockPolicyTest {

    private static final String EMAIL = "lock@vibelog.test";

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-06T00:00:00Z"));
    private final LoginLockPolicy policy = new LoginLockPolicy(now::get);

    @Test
    void 다섯_번째_실패에서_잠긴다() {
        for (int i = 1; i <= 4; i++) {
            assertThat(policy.recordFailure(EMAIL)).as("%d번째 실패", i).isFalse();
        }
        assertThat(policy.recordFailure(EMAIL)).isTrue();
        assertThat(policy.isLocked(EMAIL)).isTrue();
    }

    @Test
    void 잠긴_동안의_실패는_잠금_시간을_늘리지_않고_5분_뒤_풀린다() {
        failFiveTimes();

        later(Duration.ofMinutes(4));
        policy.recordFailure(EMAIL);
        later(Duration.ofMinutes(1));

        assertThat(policy.isLocked(EMAIL)).isFalse();
    }

    @Test
    void 잠금이_풀리면_처음부터_다시_센다() {
        failFiveTimes();
        later(Duration.ofMinutes(5));

        assertThat(policy.recordFailure(EMAIL)).isFalse();
        assertThat(policy.isLocked(EMAIL)).isFalse();
    }

    @Test
    void 성공하면_연속_실패_횟수를_지운다() {
        for (int i = 0; i < 4; i++) {
            policy.recordFailure(EMAIL);
        }
        policy.reset(EMAIL);

        assertThat(policy.recordFailure(EMAIL)).isFalse();
    }

    private void failFiveTimes() {
        for (int i = 0; i < 5; i++) {
            policy.recordFailure(EMAIL);
        }
    }

    private void later(Duration duration) {
        now.set(now.get().plus(duration));
    }
}
