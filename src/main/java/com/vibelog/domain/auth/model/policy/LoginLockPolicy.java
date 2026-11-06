package com.vibelog.domain.auth.model.policy;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/*
 * [LoginLockPolicy]
 * 역할 : 5회 연속 로그인에 실패한 이메일을 5분 동안 막는다. (PL-USR-007, NFR-SEC-005)
 * 책임 : - 실패하면 센다(LoginFailureHandler), 성공하면 지운다(LoginSuccessHandler),
 *          잠겼으면 비밀번호가 맞아도 로그인을 막는다(MemberLoginService).
 *        - 잠긴 동안의 시도는 세지 않는다. 그래야 잠금 시간이 계속 늘어나지 않는다.
 *        - 5분이 지나면 처음부터 다시 센다.
 */
@Component
public class LoginLockPolicy {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(5);

    // ponytail: 서버 메모리에 센다. 재시작하면 초기화되고, 서버를 여러 대 띄우면 서버마다 따로 센다.
    //           그렇게 되면 member 테이블 컬럼(DB 팀 합의)이나 Redis 로 옮긴다
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Supplier<Instant> now;

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

    /* 실패를 하나 센다. 이번 실패로 잠겼거나 이미 잠겨 있으면 true */
    public boolean recordFailure(String email) {
        Instant time = now.get();
        Attempt attempt = attempts.compute(email, (key, old) -> {
            if (old != null && old.lockedAt(time)) {
                return old;
            }
            int failures = (old == null || old.lockedUntil() != null) ? 1 : old.failures() + 1;
            return new Attempt(failures, failures >= MAX_FAILURES ? time.plus(LOCK_TIME) : null);
        });
        return attempt.lockedAt(time);
    }

    public void reset(String email) {
        attempts.remove(email);
    }

    private record Attempt(int failures, Instant lockedUntil) {

        boolean lockedAt(Instant time) {
            return lockedUntil != null && time.isBefore(lockedUntil);
        }
    }
}
