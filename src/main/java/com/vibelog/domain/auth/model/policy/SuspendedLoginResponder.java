package com.vibelog.domain.auth.model.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dto.SuspensionNotice;
import com.vibelog.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * [SuspendedLoginResponder]
 * 역할 : 정지 회원을 돌려보내는 응답 하나 (M-07, ACCOUNT_SUSPENDED). 폼 로그인 실패·소셜 로그인 성공·로그인 중 POST 가 같이 쓴다.
 * 책임 : - 로그인돼 있으면 로그아웃시키고 로그인 유지 쿠키도 지운다.
 *        - 화면 요청은 /login?suspended 로 보내고 M-07 에 보여 줄 값을 flash 로 넘긴다. /api 요청은 403 JSON (명세 auth.md P2).
 */
@Component
@RequiredArgsConstructor
public class SuspendedLoginResponder {

    private final LoginRememberMeServices loginRememberMeServices;
    private final ObjectMapper objectMapper;
    private final SessionFlashMapManager flashMapManager = new SessionFlashMapManager();

    public void respond(HttpServletRequest request, HttpServletResponse response, Authentication authentication,
                        SuspensionNotice notice) throws IOException {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            loginRememberMeServices.logout(request, response, authentication);
        }
        if (ErrorResponse.isApiRequest(request)) {
            response.setStatus(AuthErrorCode.ACCOUNT_SUSPENDED.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse(AuthErrorCode.ACCOUNT_SUSPENDED.name(), AuthErrorCode.ACCOUNT_SUSPENDED.getMessage()));
            return;
        }
        FlashMap flashMap = new FlashMap();
        flashMap.put("suspension", notice);
        flashMap.setTargetRequestPath("/login");
        flashMapManager.saveOutputFlashMap(flashMap, request, response);
        response.sendRedirect("/login?suspended");
    }
}
