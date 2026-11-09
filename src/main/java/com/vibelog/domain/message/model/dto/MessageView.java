package com.vibelog.domain.message.model.dto;

import java.time.LocalDateTime;

/* 쪽지함 오른쪽에 연 쪽지 (API-MSG-01 messageId). received 면 답장 폼을 보여 준다 */
public record MessageView(Long messageId, Long senderMemberId, String senderNickname, Long receiverMemberId,
                          String receiverNickname, String content, LocalDateTime createdAt, boolean received) {
}
