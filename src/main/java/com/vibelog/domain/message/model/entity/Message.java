package com.vibelog.domain.message.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [Message]
 * 역할 : 쪽지 테이블(message)과 매핑된다. 답장은 parent_message_id 가 있는 쪽지다 (FR-MSG-001·002).
 * 책임 : 컬럼은 sql/01-schema.sql 의 message 그대로 둔다. 보내기 규칙(하루 20통, 연속 3통)은 #184 에서 붙인다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    private Long senderMemberId;

    private Long receiverMemberId;

    private Long parentMessageId;

    private String content;

    private LocalDateTime readAt;

    private LocalDateTime senderDeletedAt;

    private LocalDateTime receiverDeletedAt;

    private Message(Long senderMemberId, Long receiverMemberId, Long parentMessageId, String content) {
        this.senderMemberId = senderMemberId;
        this.receiverMemberId = receiverMemberId;
        this.parentMessageId = parentMessageId;
        this.content = content;
    }

    /* parentMessageId 가 null 이면 새 쪽지, 있으면 답장 */
    public static Message create(Long senderMemberId, Long receiverMemberId, Long parentMessageId, String content) {
        return new Message(senderMemberId, receiverMemberId, parentMessageId, content);
    }
}
