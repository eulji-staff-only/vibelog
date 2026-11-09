package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.model.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
 * [MessageNotificationController]
 * 역할 : 알림 열기 (API-NTF-02). 드롭다운 항목이 이 주소 하나로 끝난다.
 * 책임 : 읽음은 GET 에서 남긴다 (명세 결정 7). 남의 알림은 NOT_OWNER, 없는 알림은 NOTIFICATION_NOT_FOUND → 오류 화면.
 */
@Controller
@RequiredArgsConstructor
public class MessageNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications/{notificationId}")
    public String open(@PathVariable Long notificationId, @AuthenticationPrincipal LoginMember loginMember) {
        return "redirect:" + notificationService.open(loginMember.getMemberId(), notificationId);
    }
}
