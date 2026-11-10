package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.course.model.policy.LoginDiagnosisChecker;
import com.vibelog.domain.lesson.model.policy.LoginEnrollmentChecker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * [LoginSuccessHandler]
 * 역할 : 로그인에 성공한 회원을 어느 화면으로 보낼지 정한다. (API-USR-05 도착지 규칙, FR-USR-004)
 * 책임 : ① redirect 가 있으면 그 화면 ② 운영자면 /admin ③ 진단 기록(마침·건너뜀)이 없으면 /diagnosis
 *        ④ 진행 중 코스 있으면 /my/learning ⑤ 그 외 /courses.
 *        ③ 은 course, ④ 는 lesson 에 계약 ⑫ 로 묻는다.
 *        redirect 는 우리 사이트 안 주소만 받는다. 다른 사이트로 보내는 피싱(open redirect)을 막기 위해서다.
 *        성공하면 연속 실패 횟수를 지운다 (PL-USR-007).
 */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginDiagnosisChecker loginDiagnosisChecker;
    private final LoginEnrollmentChecker loginEnrollmentChecker;
    private final LoginLockPolicy loginLockPolicy;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        sendToDestination(response, authentication, request.getParameter("redirect"));
    }

    /* 소셜 로그인(SocialLoginSuccessHandler)은 redirect 를 요청 값이 아니라 세션에서 꺼내 넘긴다 (#363) */
    void sendToDestination(HttpServletResponse response, Authentication authentication, String redirect) throws IOException {
        loginLockPolicy.reset(authentication.getName());
        response.sendRedirect(destination(redirect, (LoginMember) authentication.getPrincipal()));
    }

    private String destination(String redirect, LoginMember member) {
        if (isSitePath(redirect)) {
            return redirect;
        }
        if (member.isAdmin()) {
            return "/admin";
        }
        if (!loginDiagnosisChecker.hasDiagnosisRecord(member.getMemberId())) {
            return "/diagnosis";
        }
        if (loginEnrollmentChecker.hasActiveEnrollment(member.getMemberId())) {
            return "/my/learning";
        }
        return "/courses";
    }

    /*
     * "/my/learning" ⭕ / "https://evil.com", "//evil.com", "/\evil.com" ❌ (브라우저가 다른 사이트로 읽는다)
     * 탭·줄바꿈도 막는다. 브라우저가 지우고 읽어서 "/\t/evil.com" 이 "//evil.com" 이 된다.
     */
    private boolean isSitePath(String url) {
        return url != null
                && url.startsWith("/") && !url.startsWith("//") && !url.startsWith("/\\")
                && url.chars().noneMatch(Character::isISOControl);
    }
}
