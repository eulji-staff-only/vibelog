package com.vibelog.domain.message.model.dto;

/* 받는 사람 찾기 결과 (API-MSG-02 JSON [{"memberId", "nickname"}]) */
public record ReceiverItem(Long memberId, String nickname) {
}
