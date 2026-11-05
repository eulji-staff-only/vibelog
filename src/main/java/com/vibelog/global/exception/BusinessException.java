package com.vibelog.global.exception;

import lombok.Getter;

/*
 * [BusinessException]
 * 역할 : 도메인 규칙 / 비즈니스 규칙을 어겼을 때 던지는 예외. 어떤 규칙인지는 ErrorCode 로 구분한다.
 * 책임 : - 예외를 "던지기만" 한다. 화면 처리는 GlobalExceptionHandler 가 책임진다.
 *        - RuntimeException 이므로 @Transactional 메서드 안에서 발생하면 롤백된다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
