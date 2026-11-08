package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * [SocialLoginFailureHandler]
 * 역할 : 구글·깃허브 로그인에 실패하면 로그인 화면으로 돌려보낸다 (API-USR-07, #91).
 * 책임 : 같은 이메일이 다른 방식으로 가입돼 있으면(SOCIAL_EMAIL_CONFLICT) /login?socialConflict 로 "처음 가입한 방식" 안내를,
 *        그 밖(취소·이메일 없음 등)은 /login?socialError 로 다시 시도 안내를 보여 준다.
 */
@Component
public class SocialLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException {
        boolean conflict = e instanceof OAuth2AuthenticationException oauth
                && AuthErrorCode.SOCIAL_EMAIL_CONFLICT.name().equals(oauth.getError().getErrorCode());
        response.sendRedirect(conflict ? "/login?socialConflict" : "/login?socialError");
    }
}
