package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.SuspensionNotice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * [SocialLoginSuccessHandler]
 * 역할 : 구글·깃허브 로그인에 성공한 회원을 보낼 화면을 정한다 (API-USR-07, #91).
 * 책임 : 처음 로그인해 방금 가입했으면 진단(/diagnosis)으로, 아니면 이메일 로그인과 같은 도착지 규칙(LoginSuccessHandler)을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class SocialLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginSuccessHandler loginSuccessHandler;
    private final SuspensionPolicy suspensionPolicy;
    private final SuspendedLoginResponder suspendedLoginResponder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 정지 회원은 계정을 확인한 뒤 돌려보낸다 (API-USR-07 P2)
        var suspension = suspensionPolicy.findActive(((LoginMember) authentication.getPrincipal()).getMemberId());
        if (suspension.isPresent()) {
            suspendedLoginResponder.respond(request, response, authentication, SuspensionNotice.from(suspension.get()));
            return;
        }
        if (((LoginMember) authentication.getPrincipal()).isFirstLogin()) {
            response.sendRedirect("/diagnosis");
            return;
        }
        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);
    }
}
