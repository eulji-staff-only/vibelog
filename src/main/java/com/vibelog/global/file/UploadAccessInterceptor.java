package com.vibelog.global.file;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;

/*
 * [UploadAccessInterceptor]
 * 역할 : 업로드 파일 주소(/uploads/**)를 바로 열 때, 비공개 포트폴리오 파일이면 막는다 (#368, PL-SEC-002, NFR-SEC-003).
 * 책임 : - 열 수 있는지는 PortfolioReader.canOpenFile 이 정한다 (비공개·삭제·숨김 포트폴리오 스크린샷, 평가 에이전트 설정 파일).
 *          게시글 사진처럼 공개가 맞는 파일은 지금처럼 누구나 연다.
 *        - 막으면 404 (파일이 있다는 것도 알리지 않는다). 운영자는 신고 처리·과제 관리에서 봐야 하므로 막지 않는다.
 *        - API-PRT-03 설정 파일 내려받기는 과제가 열렸는지 확인한 뒤 이 주소로 forward 하므로, forward 는 다시 보지 않는다.
 *        - 주소는 디코딩한 주소(UrlPathHelper)로 본다 (%61 같은 인코딩으로 검사를 비켜 가지 못하게).
 *        - PortfolioReader 는 ObjectProvider 로 받는다. @WebMvcTest 화면 테스트에는 빈이 없어서, 없으면 검사하지 않는다.
 *        - 파일 요청마다 portfolio 를 주소로 찾는다 (idx_portfolio_screenshot_url, #483). portfolio_assignment 는 코스당 1건이라 인덱스 없이 찾는다.
 */
@Component
@RequiredArgsConstructor
public class UploadAccessInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final ObjectProvider<PortfolioReader> portfolioReader;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/uploads/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        PortfolioReader reader = portfolioReader.getIfAvailable();
        if (reader == null || request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginMember member = authentication != null && authentication.getPrincipal() instanceof LoginMember m ? m : null;
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if ((member != null && member.isAdmin()) || reader.canOpenFile(path, member == null ? null : member.getMemberId())) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }
}
