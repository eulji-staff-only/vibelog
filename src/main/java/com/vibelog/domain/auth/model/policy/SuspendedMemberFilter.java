package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.SuspensionNotice;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
 * [SuspendedMemberFilter]
 * 역할 : 로그인한 채로 정지된 회원의 POST 요청(작성·수정·삭제 등)을 막는다 (FR-USR-010 "작성이 막히고").
 * 책임 : - 화면 보기(GET)는 막지 않는다. 정지 중이면 로그아웃시키고 M-07 / 403 JSON 으로 보낸다 (SuspendedLoginResponder).
 *        - 탈퇴 요청도 여기서 먼저 막힌다. MypageService 의 SUSPENDED_CANNOT_WITHDRAW 는 두 번째 방어선이다.
 *        - Spring 이 일반 서블릿 필터로 한 번 더 등록하지 않게 @Component 를 붙이지 않고 SecurityConfig 가 만든다.
 */
@RequiredArgsConstructor
public class SuspendedMemberFilter extends OncePerRequestFilter {

    private final SuspensionPolicy suspensionPolicy;
    private final SuspendedLoginResponder suspendedLoginResponder;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginMember member) {
            var suspension = suspensionPolicy.findActive(member.getMemberId());
            if (suspension.isPresent()) {
                suspendedLoginResponder.respond(request, response, authentication, SuspensionNotice.from(suspension.get()));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
