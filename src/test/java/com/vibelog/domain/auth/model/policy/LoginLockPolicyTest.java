package com.vibelog.domain.auth.model.policy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 5회 연속 실패하면 5분 잠금 (PL-USR-007). 시간을 직접 움직여서 확인한다.
 * tryAttempt 는 비밀번호를 비교하기 전에 부르고, true 일 때만 비교한다.
 */
class LoginLockPolicyTest {

    private static final String EMAIL = "lock@vibelog.test";

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-06T00:00:00Z"));
    private final LoginLockPolicy policy = new LoginLockPolicy(now::get, 100);

    @Test
    void 다섯_번째_시도에서_잠기고_여섯_번째부터는_비교하지_않는다() {
        for (int i = 1; i <= 4; i++) {
            assertThat(policy.tryAttempt(EMAIL)).as("%d번째 시도", i).isTrue();
            assertThat(policy.isLocked(EMAIL)).isFalse();
        }
        assertThat(policy.tryAttempt(EMAIL)).isTrue();
        assertThat(policy.isLocked(EMAIL)).isTrue();
        assertThat(policy.tryAttempt(EMAIL)).isFalse();
    }

    @Test
    void 같은_이메일로_동시에_보내도_다섯_번까지만_비교한다() throws Exception {
        int requests = 50;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return policy.tryAttempt(EMAIL);
            }));
        }
        start.countDown();

        int compared = 0;
        for (Future<Boolean> result : results) {
            compared += result.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(compared).isEqualTo(5);
    }

    @Test
    void 잠긴_동안의_시도는_잠금_시간을_늘리지_않고_5분_뒤_풀린다() {
        tryFiveTimes(EMAIL);

        later(Duration.ofMinutes(4));
        assertThat(policy.tryAttempt(EMAIL)).isFalse();
        later(Duration.ofMinutes(1));

        assertThat(policy.isLocked(EMAIL)).isFalse();
    }

    @Test
    void 잠금이_풀리면_처음부터_다시_센다() {
        tryFiveTimes(EMAIL);
        later(Duration.ofMinutes(5));

        assertThat(policy.tryAttempt(EMAIL)).isTrue();
        assertThat(policy.isLocked(EMAIL)).isFalse();
    }

    @Test
    void 성공하면_연속_실패_횟수를_지운다() {
        for (int i = 0; i < 4; i++) {
            policy.tryAttempt(EMAIL);
        }
        policy.reset(EMAIL);

        policy.tryAttempt(EMAIL);
        assertThat(policy.isLocked(EMAIL)).isFalse();
    }

    @Test
    void 기록이_한도를_넘으면_잠기지_않은_기록만_지운다() {
        tryFiveTimes(EMAIL);
        for (int i = 0; i < 200; i++) {
            policy.tryAttempt("other" + i + "@vibelog.test");
        }

        assertThat(policy.size()).isLessThanOrEqualTo(100);
        assertThat(policy.isLocked(EMAIL)).isTrue();
        assertThat(policy.tryAttempt(EMAIL)).isFalse();
    }

    private void tryFiveTimes(String email) {
        for (int i = 0; i < 5; i++) {
            policy.tryAttempt(email);
        }
    }

    private void later(Duration duration) {
        now.set(now.get().plus(duration));
    }
}
