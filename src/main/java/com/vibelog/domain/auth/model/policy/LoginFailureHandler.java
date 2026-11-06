package com.vibelog.domain.auth.model.policy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
 * [LoginFailureHandler]
 * 역할 : 로그인에 실패하면 같은 폼으로 돌려보낸다. (API-USR-05, PL-USR-007)
 * 책임 : - 잠겼으면(5번째 실패부터, LoginAuthenticationProvider) /login?locked (LOGIN_LOCKED),
 *          그 전에는 /login?error (LOGIN_FAILED). 돌아갈 주소(redirect)는 유지한다.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException {
        String url = e instanceof LockedException ? "/login?locked" : "/login?error";
        String redirect = request.getParameter("redirect");
        response.sendRedirect(redirect == null || redirect.isBlank()
                ? url
                : url + "&redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
    }
}
