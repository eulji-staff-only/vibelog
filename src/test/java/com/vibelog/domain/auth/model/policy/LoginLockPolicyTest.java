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
    private final LoginLockPolicy policy = new LoginLockPolicy(now::get);

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
    void 다른_이메일_기록이_많아도_실패_횟수와_잠금을_유지한다() {
        String locked = "locked@vibelog.test";
        tryFiveTimes(locked);
        for (int i = 0; i < 4; i++) {
            policy.tryAttempt(EMAIL);
        }
        for (int i = 0; i < 20_000; i++) {
            policy.tryAttempt("other" + i + "@vibelog.test");
        }

        assertThat(policy.isLocked(locked)).isTrue();
        policy.tryAttempt(EMAIL);
        assertThat(policy.isLocked(EMAIL)).isTrue();
    }

    @Test
    void 마지막_시도부터_5분이_지난_기록은_지우고_처음부터_센다() {
        for (int i = 0; i < 4; i++) {
            policy.tryAttempt(EMAIL);
        }
        policy.tryAttempt("old@vibelog.test");

        later(Duration.ofMinutes(5));
        policy.tryAttempt(EMAIL);

        assertThat(policy.isLocked(EMAIL)).isFalse();
        assertThat(policy.size()).isEqualTo(1);
    }

    /* 잠금 남은 시간 (#394): 풀리는 시각은 잠긴 때 + 5분이고, 다시 시도해도 늦춰지지 않는다 */
    @Test
    void 잠기고_2분_뒤_남은_시간은_3분이다() {
        assertThat(policy.lockedUntil(EMAIL)).isEmpty();
        tryFiveTimes(EMAIL);
        later(Duration.ofMinutes(2));
        policy.tryAttempt(EMAIL);

        assertThat(Duration.between(now.get(), policy.lockedUntil(EMAIL).orElseThrow())).isEqualTo(Duration.ofMinutes(3));

        later(Duration.ofMinutes(3));
        assertThat(policy.lockedUntil(EMAIL)).isEmpty();
    }

    /* 주소로 받은 until 은 바꿀 수 있으므로 0 ~ 5분으로 자른다 (#394) */
    @Test
    void 남은_초는_풀리는_시각으로_계산하고_0초에서_5분_사이로_자른다() {
        long nowSeconds = now.get().getEpochSecond();
        assertThat(policy.remainingSeconds(nowSeconds + 180)).isEqualTo(180);
        assertThat(policy.remainingSeconds(nowSeconds + 100_000)).isEqualTo(300);
        assertThat(policy.remainingSeconds(nowSeconds - 10)).isZero();
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
