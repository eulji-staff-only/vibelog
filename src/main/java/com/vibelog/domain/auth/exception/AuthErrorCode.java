package com.vibelog.domain.auth.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [AuthErrorCode]
 * 역할 : 회원(auth) 영역의 비즈니스 예외 종류.
 * 책임 : 코드·상태·메시지는 API 명세 _template.md 7-3 그대로 둔다.
 *        로그인 실패(LOGIN_FAILED·LOGIN_LOCKED)는 Spring Security 가 처리하므로 여기 두지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다. 로그인해 주세요."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "구글·깃허브로 가입한 계정은 비밀번호가 없습니다."),
    SOCIAL_EMAIL_CONFLICT(HttpStatus.CONFLICT, "처음 가입한 방식(이메일·GitHub·구글)으로 로그인해 주세요."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "링크가 만료되었습니다."),
    TOKEN_USED(HttpStatus.BAD_REQUEST, "링크가 만료되었습니다."),
    RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "1분 뒤에 다시 받을 수 있습니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
