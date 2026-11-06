package com.vibelog.domain.auth.model.policy;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/*
 * [LoginLockPolicy]
 * 역할 : 5회 연속 로그인에 실패한 이메일을 5분 동안 막는다. (PL-USR-007, NFR-SEC-005)
 * 책임 : - 비밀번호를 비교하기 전에 시도를 센다(LoginAuthenticationProvider), 성공하면 지운다(LoginSuccessHandler).
 *          비교 전에 세므로 같은 이메일로 동시에 보내도 5번까지만 비교한다.
 *        - 잠긴 동안의 시도는 세지 않는다. 그래야 잠금 시간이 계속 늘어나지 않는다.
 *        - 5분이 지나면 처음부터 다시 센다.
 *        - 기록이 MAX_ENTRIES 개를 넘으면 잠기지 않은 기록을 지운다. 잠긴 기록은 남겨 잠금을 피할 수 없게 한다.
 */
@Component
public class LoginLockPolicy {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(5);
    private static final int MAX_ENTRIES = 10_000;

    // ponytail: 서버 메모리에 센다. 재시작하면 초기화되고, 서버를 여러 대 띄우면 서버마다 따로 센다.
    //           그렇게 되면 member 테이블 컬럼(DB 팀 합의)이나 Redis 로 옮긴다
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Supplier<Instant> now;
    private final int maxEntries;

    public LoginLockPolicy() {
        this(Instant::now, MAX_ENTRIES);
    }

    /* 테스트에서 시간과 기록 한도를 정하려고 둔 생성자 */
    LoginLockPolicy(Supplier<Instant> now, int maxEntries) {
        this.now = now;
        this.maxEntries = maxEntries;
    }

    public boolean isLocked(String email) {
        Attempt attempt = attempts.get(email);
        return attempt != null && attempt.lockedAt(now.get());
    }

    /* 비밀번호를 비교하기 전에 부른다. 잠겨 있으면 세지 않고 false, 아니면 시도를 하나 세고 true (5번째 시도부터 잠긴다) */
    public boolean tryAttempt(String email) {
        Instant time = now.get();
        if (attempts.size() >= maxEntries) {
            // ponytail: 잠긴 기록이 한도보다 많으면 시도마다 전체를 훑는다. 그만큼 공격받으면 IP 단위 제한을 붙인다
            attempts.values().removeIf(attempt -> !attempt.lockedAt(time));
        }
        AtomicBoolean allowed = new AtomicBoolean();
        attempts.compute(email, (key, old) -> {
            if (old != null && old.lockedAt(time)) {
                return old;
            }
            allowed.set(true);
            int failures = (old == null || old.lockedUntil() != null) ? 1 : old.failures() + 1;
            return new Attempt(failures, failures >= MAX_FAILURES ? time.plus(LOCK_TIME) : null);
        });
        return allowed.get();
    }

    public void reset(String email) {
        attempts.remove(email);
    }

    int size() {
        return attempts.size();
    }

    private record Attempt(int failures, Instant lockedUntil) {

        boolean lockedAt(Instant time) {
            return lockedUntil != null && time.isBefore(lockedUntil);
        }
    }
}
