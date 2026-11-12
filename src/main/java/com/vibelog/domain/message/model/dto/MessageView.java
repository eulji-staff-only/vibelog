package com.vibelog.domain.message.model.dto;

import java.time.LocalDateTime;

/*
 * 쪽지함 오른쪽에 연 쪽지 (API-MSG-01 messageId). received 면 답장 폼을 보여 준다.
 * hidden 이면 운영자가 숨긴 쪽지다. 받은 사람에게는 content 가 숨김 문구, 보낸 사람에게는 내용 + 숨김 안내 (FR-RPT-003)
 */
public record MessageView(Long messageId, Long senderMemberId, String senderNickname, Long receiverMemberId,
                          String receiverNickname, String content, LocalDateTime createdAt, boolean received,
                          boolean hidden) {
}
