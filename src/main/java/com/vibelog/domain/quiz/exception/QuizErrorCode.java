package com.vibelog.domain.quiz.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [QuizErrorCode]
 * 역할 : quiz 영역(퀴즈 풀이·채점)의 예외 종류.
 * 책임 : API 명세 7장 공통 에러 코드 중 퀴즈 풀이에 해당하는 것과 이름·상태·문구를 맞춘다 (docs/design/api/_template.md).
 *        문항 등록(#102) 같은 다른 기능의 코드는 쓰는 이슈에서 추가한다. 남의 풀이는 공통 NOT_OWNER 를 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum QuizErrorCode implements ErrorCode {

    ALREADY_CHECKED(HttpStatus.CONFLICT, "이미 확인한 문항입니다."),
    ATTEMPT_SUBMITTED(HttpStatus.CONFLICT, "이미 제출했습니다."),
    QUIZ_NOT_FINISHED(HttpStatus.BAD_REQUEST, "모든 문항을 확인한 뒤 제출할 수 있습니다."),
    /* 퀴즈 화면·풀이 시작에서 활동이 열려 있지 않을 때 (API-LSN-03·04). 화면은 "수업 화면 + 토스트"(③)로 보낸다 */
    ACTIVITY_LOCKED(HttpStatus.FORBIDDEN, "앞 퀴즈를 정답률 50% 이상으로 통과하면 열립니다."),
    /* 이 풀이의 퀴즈 문항이 아니거나, 그 문항의 보기가 아닐 때 (API-LSN-05). 문구는 GlobalExceptionHandler 와 같다 */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요.");

    private final HttpStatus status;
    private final String message;
}
