package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.model.dto.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.stereotype.Component;

import java.time.Duration;

/*
 * [LoginRememberMeServices]
 * 역할 : 로그인 폼에서 "로그인 유지"(rememberMe)를 체크하면 14일 동안 로그인을 유지하는 쿠키를 만든다. (PL-USR-006)
 * 책임 : - 쿠키에는 이메일·만료 시각과, 비밀번호 해시와 서버 키로 만든 서명이 들어간다 (Spring Security 기본 방식, 테이블 필요 없음).
 *          비밀번호를 바꾸면 이전 쿠키는 쓸 수 없다.
 *        - 운영자는 체크해도 쿠키를 만들지 않는다 (PL-USR-006).
 *        - 서버 키는 환경 변수 REMEMBER_ME_KEY 로 받는다. 없으면 켤 때마다 무작위 키라서, 서버를 다시 켜면 로그인 유지가 풀린다.
 *          로컬 개발은 이걸로 충분하고, 배포 서버에는 REMEMBER_ME_KEY 를 넣는다.
 */
@Component
public class LoginRememberMeServices extends TokenBasedRememberMeServices {

    private final SuspensionPolicy suspensionPolicy;

    public LoginRememberMeServices(@Value("${REMEMBER_ME_KEY:#{T(java.util.UUID).randomUUID().toString()}}") String key,
                                   UserDetailsService userDetailsService, SuspensionPolicy suspensionPolicy) {
        super(key, userDetailsService);
        this.suspensionPolicy = suspensionPolicy;
        setParameter("rememberMe");     // API-USR-05 form 이름
        setTokenValiditySeconds((int) Duration.ofDays(14).toSeconds());
    }

    /* 쿠키로 자동 로그인할 때도 정지 회원이면 거부한다. 쿠키는 부모 클래스가 지운다 (API-USR-05 P2) */
    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request,
                                                 HttpServletResponse response) {
        UserDetails user = super.processAutoLoginCookie(cookieTokens, request, response);
        if (suspensionPolicy.isSuspended(((LoginMember) user).getMemberId())) {
            throw new InvalidCookieException("이용 정지 회원");
        }
        return user;
    }

    @Override
    public void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
                               Authentication authentication) {
        if (((LoginMember) authentication.getPrincipal()).isAdmin()) {
            return;
        }
        super.onLoginSuccess(request, response, authentication);
    }
}
