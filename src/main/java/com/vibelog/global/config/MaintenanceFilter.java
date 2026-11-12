package com.vibelog.global.config;

import com.vibelog.global.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * [MaintenanceFilter]
 * 역할 : 점검 중(환경 변수 MAINTENANCE=true)이면 모든 요청에 503 + E-04 점검 중 화면을 보낸다 (PL-ERR-006, API 명세 7-2, #365).
 * 책임 : - 점검 중일 때만 빈으로 만들어진다. 켜고 끄려면 환경 변수를 바꾸고 앱을 다시 시작한다.
 *        - 가장 먼저 돌아 로그인·DB 를 거치지 않는다. 화면은 정적 파일 static/maintenance.html, /api/** 는 JSON {code, message}.
 *        - 점검 화면이 쓰는 CSS(/css/**)는 그대로 보낸다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "vibelog.maintenance", havingValue = "true")
public class MaintenanceFilter extends OncePerRequestFilter {

    private static final byte[] JSON = "{\"code\":\"MAINTENANCE\",\"message\":\"서비스 점검 중입니다.\"}".getBytes(StandardCharsets.UTF_8);

    private final byte[] page;

    public MaintenanceFilter() throws IOException {
        this.page = new ClassPathResource("static/maintenance.html").getContentAsByteArray();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/css/")) {
            chain.doFilter(request, response);
            return;
        }
        boolean api = ErrorResponse.isApiRequest(request);
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType((api ? MediaType.APPLICATION_JSON : MediaType.TEXT_HTML).toString());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(api ? JSON : page);
    }
}
