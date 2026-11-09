package com.vibelog.domain.message.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [MessageErrorCode]
 * 역할 : 쪽지·알림·공지(message) 영역의 비즈니스 예외 종류.
 * 책임 : 코드·상태·메시지는 API 명세 _template.md 7-3 그대로 둔다. 쓰는 곳이 생길 때 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum MessageErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
