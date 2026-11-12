package com.vibelog.domain.message.model.entity;

/*
 * [NotificationType]
 * 역할 : 알림 종류 (notification.notification_type, DB 설계서 4-3). HIDDEN 은 P3 신고에서 쓴다.
 */
public enum NotificationType {
    RECOMMEND, COMMENT, REPLY, REVIEW, MESSAGE, BADGE, NOTICE, HIDDEN
}
