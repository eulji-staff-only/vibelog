package com.vibelog.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [CommonErrorCode]
 * 역할 : 여러 영역이 같이 쓰는 공통 에러. 코드·상태·메시지는 API 명세 _template.md 7-2 그대로 둔다.
 * 책임 : - 7-2 표에서 Service 가 BusinessException 으로 던지는 것(BANNED_WORD, INVALID_INPUT, NOT_OWNER, EMAIL_NOT_VERIFIED 등)만
 *          쓰는 곳이 생길 때 여기에 추가한다.
 *        - LOGIN_REQUIRED·ADMIN_ONLY 는 SecurityConfig 가, 없는 주소(404)·서버 오류(500)는 GlobalExceptionHandler 가 처리하므로 여기 두지 않는다.
 *        - 한 영역에만 속하는 에러(7-3 표)는 각 영역 enum 에 둔다.
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    BANNED_WORD(HttpStatus.BAD_REQUEST, "사용할 수 없는 단어가 들어 있습니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "JPG·PNG·WebP 파일만, 장당 5MB까지 올릴 수 있습니다. (설정 파일은 md·json·yaml·txt)"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "본인만 할 수 있습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증을 마치면 글을 쓸 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
