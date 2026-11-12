package com.vibelog.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/*
 * [RequestLogFilter]
 * 역할 : 요청마다 요청 ID를 붙이고, 끝나면 "방식 주소 상태 걸린시간" 한 줄을 남긴다 (코드 점검표 C-5·C-6).
 * 책임 : - 요청 ID(8자리)는 MDC 에 넣어 이 요청 중 찍힌 모든 로그 줄에 [요청ID]로 붙는다 (application.yaml logging.pattern.level).
 *          그래서 동시에 들어온 요청의 로그도 요청 ID로 묶어 볼 수 있다.
 *        - 주소는 경로만 남기고 쿼리 문자열(?token= 등)은 남기지 않는다 (토큰·검색어가 로그에 남지 않게).
 *        - 정적 파일(/css, /js, /images 등 확장자가 있는 주소)은 기록하지 않는다.
 *        - 점검 필터(MaintenanceFilter) 바로 다음, Spring Security 보다 먼저 돌아 로그인 실패·403 도 기록한다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLogFilter extends OncePerRequestFilter {

    static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, 8));
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(),
                    (System.nanoTime() - start) / 1_000_000);
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.lastIndexOf('.') > uri.lastIndexOf('/');
    }
}
