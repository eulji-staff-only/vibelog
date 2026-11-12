package com.vibelog.domain.auth.model.policy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
 * [LoginFailureHandler]
 * 역할 : 로그인에 실패하면 같은 폼으로 돌려보낸다. (API-USR-05, PL-USR-007)
 * 책임 : - 잠겼으면(5번째 실패부터, LoginAuthenticationProvider) /login?locked (LOGIN_LOCKED),
 *          소셜 가입 이메일이면 /login?social (SOCIAL_ACCOUNT_EMAIL, #91),
 *          그 전에는 /login?error (LOGIN_FAILED). 돌아갈 주소(redirect)는 유지한다.
 *        - 잠금은 풀리는 시각(until, epoch 초)을 함께 넘긴다. 남은 초가 아니라 시각이라 새로고침해도 실제 남은 시간이 보인다 (#394).
 */
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final SuspendedLoginResponder suspendedLoginResponder;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException {
        if (e instanceof LoginAuthenticationProvider.SuspendedLoginException suspended) {
            suspendedLoginResponder.respond(request, response, null, suspended.getNotice());
            return;
        }
        String url = e instanceof LoginAuthenticationProvider.LoginLockedException locked && locked.getLockedUntil() != null
                ? "/login?locked&until=" + locked.getLockedUntil().getEpochSecond()
                : e instanceof LockedException ? "/login?locked"
                : e instanceof LoginAuthenticationProvider.SocialAccountLoginException ? "/login?social"
                : "/login?error";
        String redirect = request.getParameter("redirect");
        response.sendRedirect(redirect == null || redirect.isBlank()
                ? url
                : url + "&redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
    }
}
