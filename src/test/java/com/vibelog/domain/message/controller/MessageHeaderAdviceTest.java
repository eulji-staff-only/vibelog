package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.model.service.NoticeService;
import com.vibelog.domain.message.model.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/* 헤더 값은 화면 요청에서만 조회하고, 헤더를 그리지 않는 JSON API(/api/**) 요청에서는 DB 를 부르지 않는다 */
class MessageHeaderAdviceTest {

    @Test
    @SuppressWarnings("unchecked")
    void API_요청에서는_알림_수와_공지_배너를_조회하지_않는다() {
        NotificationService notificationService = mock(NotificationService.class);
        NoticeService noticeService = mock(NoticeService.class);
        ObjectProvider<NotificationService> notifications = mock(ObjectProvider.class);
        ObjectProvider<NoticeService> notices = mock(ObjectProvider.class);
        given(notifications.getIfAvailable()).willReturn(notificationService);
        given(notices.getIfAvailable()).willReturn(noticeService);
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/notifications");
        api.setServletPath("/api/notifications");

        MessageHeaderAdvice advice = new MessageHeaderAdvice(notifications, notices);

        assertThat(advice.unreadNotificationCount(loginMember, api)).isNull();
        assertThat(advice.noticeBanner(api)).isNull();
        assertThat(advice.badgeCelebration(loginMember, api)).isNull();
        verifyNoInteractions(notificationService, noticeService);
    }
}
