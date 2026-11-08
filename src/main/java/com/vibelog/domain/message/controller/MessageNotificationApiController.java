package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.model.dto.NotificationPage;
import com.vibelog.domain.message.model.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/*
 * [MessageNotificationApiController]
 * 역할 : 헤더 알림 드롭다운이 부르는 JSON (API-NTF-01 목록, API-NTF-03 모두 읽음).
 * 책임 : 회원만 들어온다 (SecurityConfig). 로그인 회원 ID 만 Service 에 넘긴다.
 */
@RestController
@RequiredArgsConstructor
public class MessageNotificationApiController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    public NotificationPage list(@RequestParam(defaultValue = "1") int page, @AuthenticationPrincipal LoginMember loginMember) {
        return notificationService.getPage(loginMember.getMemberId(), page);
    }

    @PostMapping("/api/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal LoginMember loginMember) {
        notificationService.readAll(loginMember.getMemberId());
    }
}
