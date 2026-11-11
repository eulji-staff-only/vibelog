package com.vibelog.domain.auth.model.policy;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/*
 * [LoginLockPolicy]
 * 역할 : 5회 연속 로그인에 실패한 이메일을 5분 동안 막는다. (PL-USR-007, NFR-SEC-005)
 * 책임 : - 비밀번호를 비교하기 전에 시도를 센다(LoginAuthenticationProvider), 성공하면 지운다(LoginSuccessHandler).
 *          비교 전에 세므로 같은 이메일로 동시에 보내도 5번까지만 비교한다.
 *        - 잠긴 동안의 시도는 세지 않는다. 그래야 잠금 시간이 계속 늘어나지 않는다.
 *        - 마지막 시도(잠겼으면 잠금이 풀린 때)부터 5분이 지나면 처음부터 다시 세고, 1분마다 그런 기록을 지운다.
 *          잠긴 동안에는 세지 않으므로 이렇게 해도 5분에 5번보다 많이 시도할 수 없다.
 *          그 전의 실패 횟수와 잠금은 다른 이메일 기록이 아무리 많아도 지우지 않는다.
 */
@Component
public class LoginLockPolicy {

    private static final int MAX_FAILURES = 5;
    public static final Duration LOCK_TIME = Duration.ofMinutes(5);
    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

    // ponytail: 서버 메모리에 센다. 재시작하면 초기화되고, 서버를 여러 대 띄우면 서버마다 따로 센다.
    //           그렇게 되면 member 테이블 컬럼(DB 팀 합의)이나 Redis 로 옮긴다
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Supplier<Instant> now;
    private volatile Instant nextSweep = Instant.MIN;

    public LoginLockPolicy() {
        this(Instant::now);
    }

    /* 테스트에서 시간을 움직이려고 둔 생성자 */
    LoginLockPolicy(Supplier<Instant> now) {
        this.now = now;
    }

    public boolean isLocked(String email) {
        Attempt attempt = attempts.get(email);
        return attempt != null && attempt.lockedAt(now.get());
    }

    /* 잠금이 풀리는 시각 (#394). 잠기지 않았으면 empty. 잠긴 동안의 시도는 세지 않으므로 다시 시도해도 늦춰지지 않는다 */
    public Optional<Instant> lockedUntil(String email) {
        Attempt attempt = attempts.get(email);
        return attempt != null && attempt.lockedAt(now.get()) ? Optional.of(attempt.lockedUntil()) : Optional.empty();
    }

    /* 비밀번호를 비교하기 전에 부른다. 잠겨 있으면 세지 않고 false, 아니면 시도를 하나 세고 true (5번째 시도부터 잠긴다) */
    public boolean tryAttempt(String email) {
        Instant time = now.get();
        if (!time.isBefore(nextSweep)) {
            nextSweep = time.plus(SWEEP_INTERVAL);
            attempts.values().removeIf(attempt -> attempt.expiredAt(time));
        }
        AtomicBoolean allowed = new AtomicBoolean();
        attempts.compute(email, (key, old) -> {
            if (old != null && old.lockedAt(time)) {
                return old;
            }
            allowed.set(true);
            int failures = (old == null || old.expiredAt(time)) ? 1 : old.failures() + 1;
            return new Attempt(failures, time, failures >= MAX_FAILURES ? time.plus(LOCK_TIME) : null);
        });
        return allowed.get();
    }

    public void reset(String email) {
        attempts.remove(email);
    }

    int size() {
        return attempts.size();
    }

    private record Attempt(int failures, Instant lastTriedAt, Instant lockedUntil) {

        boolean lockedAt(Instant time) {
            return lockedUntil != null && time.isBefore(lockedUntil);
        }

        /* 잠긴 기록은 마지막 시도 + 5분 = 잠금이 풀리는 때 */
        boolean expiredAt(Instant time) {
            return !time.isBefore(lastTriedAt.plus(LOCK_TIME));
        }
    }
}
