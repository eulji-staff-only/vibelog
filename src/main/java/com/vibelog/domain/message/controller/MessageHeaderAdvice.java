package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.model.dto.NoticeBanner;
import com.vibelog.domain.message.model.service.NoticeService;
import com.vibelog.domain.message.model.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/*
 * [MessageHeaderAdvice]
 * 역할 : 모든 화면의 헤더(common/layout.html)가 쓰는 알림 값을 모델에 넣는다 (message 명세 "헤더에 보여줄 값").
 * 책임 : 회원이면 안 읽은 알림 수, 비회원이면 null. 공지 배너(noticeBanner)는 누구에게나 기간 안의 배너 1개, 없으면 null.
 *        Service 는 ObjectProvider 로 받는다. @WebMvcTest 화면 테스트에는 Service 빈이 없어서, 없으면 null 로 넘어간다.
 *        JSON API(/api/**, README "JSON API 약속")는 헤더를 그리지 않으므로 조회하지 않고 null 이다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class MessageHeaderAdvice {

    private final ObjectProvider<NotificationService> notificationService;
    private final ObjectProvider<NoticeService> noticeService;

    @ModelAttribute("unreadNotificationCount")
    public Long unreadNotificationCount(@AuthenticationPrincipal LoginMember loginMember, HttpServletRequest request) {
        NotificationService service = notificationService.getIfAvailable();
        return loginMember == null || service == null || isApi(request) ? null : service.countUnread(loginMember.getMemberId());
    }

    @ModelAttribute("noticeBanner")
    public NoticeBanner noticeBanner(HttpServletRequest request) {
        NoticeService service = noticeService.getIfAvailable();
        return service == null || isApi(request) ? null : service.getShowingBanner();
    }

    private static boolean isApi(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/");
    }
}
