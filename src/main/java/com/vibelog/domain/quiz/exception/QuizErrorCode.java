package com.vibelog.domain.quiz.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [QuizErrorCode]
 * 역할 : quiz 영역(퀴즈 풀이·채점)의 예외 종류.
 * 책임 : API 명세 7장 공통 에러 코드 중 퀴즈 풀이에 해당하는 것과 이름·상태·문구를 맞춘다 (docs/design/api/_template.md).
 *        문항 등록(#102) 같은 다른 기능의 코드는 쓰는 이슈에서 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum QuizErrorCode implements ErrorCode {

    ALREADY_CHECKED(HttpStatus.CONFLICT, "이미 확인한 문항입니다."),
    ATTEMPT_SUBMITTED(HttpStatus.CONFLICT, "이미 제출했습니다."),
    QUIZ_NOT_FINISHED(HttpStatus.BAD_REQUEST, "모든 문항을 확인한 뒤 제출할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
