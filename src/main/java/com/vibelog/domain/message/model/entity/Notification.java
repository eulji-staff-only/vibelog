package com.vibelog.domain.message.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [Notification]
 * 역할 : 알림 테이블(notification)과 매핑된다. 받는 회원(member_id)에게 보이는 사이트 안 알림 한 건 (FR-NTF-001).
 * 책임 : - 컬럼은 sql/01-schema.sql 의 notification 그대로 둔다.
 *        - 문구(content)는 알림 보내기 계약(NotificationSender)이 만든다. 누르면 갈 곳은 target_type + target_id 로 정한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    private NotificationTargetType targetType;

    private Long targetId;

    private String content;

    private LocalDateTime readAt;

    private Notification(Long memberId, NotificationType notificationType, NotificationTargetType targetType,
                         Long targetId, String content) {
        this.memberId = memberId;
        this.notificationType = notificationType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.content = content;
    }

    public static Notification create(Long memberId, NotificationType notificationType, NotificationTargetType targetType,
                                      Long targetId, String content) {
        return new Notification(memberId, notificationType, targetType, targetId, content);
    }
}
