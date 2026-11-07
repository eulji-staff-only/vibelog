package com.vibelog.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibelog.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
 * [SecurityConfig]
 * 역할 : 로그인(폼 로그인)·로그아웃과 화면별 접근 권한(비회원 / 회원 / 운영자)을 정한다. (#12)
 * 책임 : - 접근 권한은 화면 흐름 설계서 0-3·1-1·1-3 표와 PL-USR-008 을 따른다.
 *          여기서는 "로그인 했나 / 운영자인가"만 본다. 이메일 인증·학습 조건(수업 열림 등)은 각 영역 Service 가 검사한다.
 *        - 비회원이 회원 화면에 오면 /login?redirect={원래 주소}, 학습자가 /admin 에 오면 메인으로 보낸다. (API 명세 0-2, 7-2)
 *        - CSRF 는 켜 둔다. 폼을 th:action 으로 만들면 토큰이 자동으로 들어간다. JSON 요청은 헤더로 보낸다 (README "JSON API").
 *        - /api/** 요청은 비로그인 401 LOGIN_REQUIRED, 권한 없음 403 도 JSON {code, message} 로 답한다 (D-08).
 *        - 로그인 규칙은 auth 영역이 정한다: 성공 후 도착지(LoginSuccessHandler), 5회 잠금(LoginAuthenticationProvider), 실패 안내(LoginFailureHandler),
 *          로그인 유지(LoginRememberMeServices). 2시간 동안 요청이 없으면 세션이 끝난다 (application.yaml, PL-USR-006).
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationSuccessHandler loginSuccessHandler;
    private final AuthenticationFailureHandler loginFailureHandler;
    private final RememberMeServices loginRememberMeServices;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 위에서부터 처음 맞는 규칙 하나만 쓴다
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        // 전체(비회원 포함): 메인, 가입·로그인·이메일 인증·비밀번호 찾기 (API-USR-01~14)
                        .requestMatchers("/", "/signup", "/login", "/auth/verify", "/password/find", "/password/reset",
                                "/api/auth/verification-mail", "/error").permitAll()
                        // 글쓰기는 회원만. 아래 "/community/*" 보다 먼저 적어야 막힌다
                        .requestMatchers("/community/write").authenticated()
                        // 전체: 트랙·코스 소개, 게시판 목록·상세 (PL-USR-008). 보기(GET)만 연다
                        .requestMatchers(HttpMethod.GET, "/tracks/*", "/courses", "/courses/*", "/community", "/community/*").permitAll()
                        // 나머지는 모두 회원
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler))
                // 로그인 유지(rememberMe) 쿠키. 로그아웃하면 쿠키도 지운다
                .rememberMe(remember -> remember.rememberMeServices(loginRememberMeServices))
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::sendToLogin)
                        .accessDeniedHandler(this::denyAccess));
        return http.build();
    }

    /* 비밀번호는 BCrypt 해시로 저장·비교한다 (NFR-SEC-001). sql/02-seed.sql 테스트 계정도 BCrypt(cost 10) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* 비회원(세션 만료 포함)이 회원 화면에 옴 → 로그인 후 돌아오도록 원래 주소를 붙여 로그인으로 보낸다 */
    private void sendToLogin(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException e) throws IOException {
        if (ErrorResponse.isApiRequest(request)) {
            sendJson(response, HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다.");
            return;
        }
        // POST 주소로는 돌아갈 수 없으므로(화면이 아님) 화면(GET) 요청일 때만 원래 주소를 붙인다
        if (!HttpMethod.GET.matches(request.getMethod())) {
            response.sendRedirect("/login");
            return;
        }
        String query = request.getQueryString();
        String original = request.getRequestURI() + (query == null ? "" : "?" + query);
        response.sendRedirect("/login?redirect=" + URLEncoder.encode(original, StandardCharsets.UTF_8));
    }

    /*
     * 학습자가 백오피스에 옴 → 표시 없이 메인으로 (ADMIN_ONLY, FR-ADM-001). 그 밖(CSRF 토큰 없음 등)은 403.
     * /api 요청은 JSON 으로: 운영자 API 는 ADMIN_ONLY, 그 밖(대개 CSRF 토큰 없음·만료)은 FORBIDDEN (7장 표에 이름이 없어 D-08 에서 정함)
     * CSRF 실패는 운영자도 겪으므로 권한 부족(ADMIN_ONLY·메인으로)으로 보지 않는다
     */
    private void denyAccess(HttpServletRequest request, HttpServletResponse response,
                            AccessDeniedException e) throws IOException {
        String uri = request.getRequestURI();
        boolean csrf = e instanceof CsrfException;
        if (ErrorResponse.isApiRequest(request)) {
            if (!csrf && uri.startsWith("/api/admin/")) {
                sendJson(response, HttpStatus.FORBIDDEN, "ADMIN_ONLY", "운영자만 쓸 수 있습니다.");
            } else {
                sendJson(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "요청을 처리할 수 없습니다. 화면을 새로고침한 뒤 다시 시도해 주세요.");
            }
            return;
        }
        if (!csrf && uri.startsWith("/admin")) {
            response.sendRedirect("/");
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private void sendJson(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
}
