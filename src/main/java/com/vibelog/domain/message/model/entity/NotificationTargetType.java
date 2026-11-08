package com.vibelog.domain.message.model.entity;

/*
 * [NotificationTargetType]
 * 역할 : 알림을 누르면 갈 대상의 종류 (notification.target_type, _template.md 10장 2번 표).
 */
public enum NotificationTargetType {
    POST, COMMENT, PORTFOLIO, REVIEW, MESSAGE, CONTEST_AWARD, NOTICE
}
