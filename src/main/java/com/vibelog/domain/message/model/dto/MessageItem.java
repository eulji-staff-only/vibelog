package com.vibelog.domain.message.model.dto;

import java.time.LocalDateTime;

/* 쪽지함 목록 한 줄 (API-MSG-01): 상대 닉네임, 내용 앞부분, 보낸 시각, (받은 쪽지면) 읽음 */
public record MessageItem(Long messageId, Long otherMemberId, String otherNickname, String preview, LocalDateTime createdAt,
                          boolean read) {

    static final int PREVIEW_LENGTH = 40;

    public static String preview(String content) {
        String oneLine = content.replaceAll("\s+", " ").strip();
        return oneLine.length() <= PREVIEW_LENGTH ? oneLine : oneLine.substring(0, PREVIEW_LENGTH) + "…";
    }
}
