package com.vibelog.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [CommonErrorCode]
 * 역할 : 여러 영역이 같이 쓰는 공통 에러. 코드·상태·메시지는 API 명세 _template.md 7-2 그대로 둔다.
 * 책임 : 한 영역에만 속하는 에러는 각 영역 enum 에 둔다. 여기는 7-2 표에 있는 것만, 쓰는 곳이 생길 때 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    BANNED_WORD(HttpStatus.BAD_REQUEST, "사용할 수 없는 단어가 들어 있습니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "JPG·PNG·WebP 파일만, 장당 5MB까지 올릴 수 있습니다. (설정 파일은 md·json·yaml·txt)");

    private final HttpStatus status;
    private final String message;
}
