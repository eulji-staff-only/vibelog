package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.SuspensionNotice;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import lombok.Getter;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/*
 * [LoginAuthenticationProvider]
 * 역할 : 폼 로그인에서 비밀번호를 비교하기 전에 5회 연속 실패 잠금을 적용한다. (PL-USR-007, NFR-SEC-005)
 * 책임 : - 비교 전에 시도를 센다(LoginLockPolicy). 같은 이메일로 동시에 보내도 5번까지만 비교한다.
 *        - 잠겨 있거나 이번 실패로 잠기면 LockedException → LoginFailureHandler 가 /login?locked 로 보낸다.
 *        - 없는 이메일도 똑같이 세고 잠근다. 그래야 잠금 안내로 가입 여부가 드러나지 않는다.
 *        - 로그인 유지(remember-me) 자동 로그인은 비밀번호를 비교하지 않으므로 여기를 거치지 않는다.
 */
@Component
public class LoginAuthenticationProvider extends DaoAuthenticationProvider {

    private final MemberRepository memberRepository;
    private final LoginLockPolicy loginLockPolicy;
    private final SuspensionPolicy suspensionPolicy;

    public LoginAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                                       MemberRepository memberRepository, LoginLockPolicy loginLockPolicy,
                                       SuspensionPolicy suspensionPolicy) {
        super(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        this.memberRepository = memberRepository;
        this.loginLockPolicy = loginLockPolicy;
        this.suspensionPolicy = suspensionPolicy;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String key = lockKey(authentication.getName());
        if (!loginLockPolicy.tryAttempt(key)) {
            throw new LockedException("로그인 잠김");
        }
        try {
            return super.authenticate(authentication);
        } catch (SuspendedLoginException e) {
            loginLockPolicy.reset(key);     // 비밀번호는 맞았으므로 실패로 세지 않는다
            throw e;
        } catch (AuthenticationException e) {
            if (loginLockPolicy.isLocked(key)) {
                throw new LockedException("로그인 잠김", e);
            }
            throw e;
        }
    }

    /* 구글·깃허브로 가입한 이메일(비밀번호 없음)로 폼 로그인하면 "처음 가입한 방식" 안내를 보여 준다 (SOCIAL_ACCOUNT_EMAIL, PL-USR-005, #91) */
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
        if (userDetails.getPassword().isEmpty()) {
            throw new SocialAccountLoginException();
        }
        super.additionalAuthenticationChecks(userDetails, authentication);
        // 정지 여부는 비밀번호가 맞은 뒤에 본다. 먼저 보면 비밀번호 없이 계정이 있는지·정지됐는지 알 수 있다 (명세 auth.md P2)
        suspensionPolicy.findActive(((LoginMember) userDetails).getMemberId()).ifPresent(suspension -> {
            throw new SuspendedLoginException(SuspensionNotice.from(suspension));
        });
    }

    /* 이용 정지 중 (FR-USR-010). LoginFailureHandler 가 M-07 로 보낸다 */
    @Getter
    public static class SuspendedLoginException extends AuthenticationException {
        private final transient SuspensionNotice notice;

        public SuspendedLoginException(SuspensionNotice notice) {
            super("이용 정지");
            this.notice = notice;
        }
    }

    public static class SocialAccountLoginException extends AuthenticationException {
        public SocialAccountLoginException() {
            super("소셜 가입 계정");
        }
    }

    /*
     * DB 는 대소문자·악센트만 다른 이메일도 같은 회원으로 찾는다 (utf8mb4_0900_ai_ci).
     * 입력값 그대로 세면 "A@x.com", "á@x.com" 처럼 바꿔 가며 잠금을 피할 수 있으므로, 가입된 이메일이면 DB 에 저장된 값으로 센다.
     * 입력값은 인증 필터가 다듬은(trim) 그대로 쓴다. 따로 다듬으면 비밀번호를 비교하는 회원과 세는 회원이 달라질 수 있다.
     */
    private String lockKey(String email) {
        return memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE)
                .map(Member::getEmail)
                .orElse(email);
    }
}
