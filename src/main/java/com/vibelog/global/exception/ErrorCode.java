package com.vibelog.global.exception;

import org.springframework.http.HttpStatus;

/*
 * [ErrorCode]
 * 역할 : 비즈니스 예외의 종류(HTTP 상태코드 + 사용자에게 보여줄 메시지)를 나타내는 약속.
 * 책임 : 각 도메인은 이 인터페이스를 구현한 enum 을 자기 패키지에 만든다.
 *        (예: domain/quiz/exception/QuizErrorCode)
 *        모든 도메인이 enum 하나를 함께 고치면 PR 마다 같은 파일에서 충돌이 나기 때문이다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getMessage();
}
