package com.vibelog.domain.message.model.dto;

import java.util.List;

/* API-NTF-01 응답 {"unreadCount", "hasNext", "items"} */
public record NotificationPage(long unreadCount, boolean hasNext, List<NotificationItem> items) {
}
