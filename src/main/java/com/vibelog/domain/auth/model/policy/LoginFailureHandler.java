package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
 * [LoginFailureHandler]
 * 역할 : 로그인에 실패하면 실패를 세고 같은 폼으로 돌려보낸다. (API-USR-05, PL-USR-007)
 * 책임 : - 5번째 실패부터는 /login?locked (LOGIN_LOCKED), 그 전에는 /login?error (LOGIN_FAILED). 돌아갈 주소(redirect)는 유지한다.
 *        - 없는 이메일도 똑같이 세고 잠근다. 그래야 잠금 안내로 가입 여부가 드러나지 않는다.
 */
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final MemberRepository memberRepository;
    private final LoginLockPolicy loginLockPolicy;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException {
        boolean locked = loginLockPolicy.recordFailure(lockKey(request.getParameter("email")));
        String url = locked ? "/login?locked" : "/login?error";
        String redirect = request.getParameter("redirect");
        response.sendRedirect(redirect == null || redirect.isBlank()
                ? url
                : url + "&redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
    }

    /*
     * DB 는 대소문자·악센트만 다른 이메일도 같은 회원으로 찾는다 (utf8mb4_0900_ai_ci).
     * 입력값 그대로 세면 "A@x.com", "á@x.com" 처럼 바꿔 가며 잠금을 피할 수 있으므로, 가입된 이메일이면 DB 에 저장된 값으로 센다.
     */
    private String lockKey(String email) {
        String input = email == null ? "" : email.strip();
        return memberRepository.findByEmailAndStatus(input, MemberStatus.ACTIVE)
                .map(Member::getEmail)
                .orElse(input);
    }
}
