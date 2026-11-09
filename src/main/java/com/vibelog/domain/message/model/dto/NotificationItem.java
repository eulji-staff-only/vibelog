package com.vibelog.domain.message.model.dto;

import com.vibelog.domain.message.model.entity.Notification;
import com.vibelog.domain.message.model.entity.NotificationType;

import java.time.LocalDateTime;

/* API-NTF-01 알림 목록의 한 줄 */
public record NotificationItem(Long notificationId, NotificationType notificationType, String content, boolean read,
                               LocalDateTime createdAt) {

    public static NotificationItem from(Notification notification) {
        return new NotificationItem(notification.getNotificationId(), notification.getNotificationType(),
                notification.getContent(), notification.isRead(), notification.getCreatedAt());
    }
}
