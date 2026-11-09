package com.vibelog.domain.exam.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [ExamErrorCode]
 * 역할 : exam 영역의 비즈니스 예외 종류.
 * 책임 : 상태·문구는 docs/design/api/_template.md 7-3 "업무별" 표 그대로 쓴다. (PL-ERR-001)
 */
@Getter
@RequiredArgsConstructor
public enum ExamErrorCode implements ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요."),
    EXAM_LOCKED(HttpStatus.FORBIDDEN, "이 수업의 마지막 퀴즈를 통과하면 열립니다."),
    REPORT_NOT_VIEWED(HttpStatus.CONFLICT, "지난 응시의 리포트를 먼저 확인해 주세요."),
    ATTEMPT_SUBMITTED(HttpStatus.CONFLICT, "이미 제출했습니다."),
    EXAM_NOT_SUBMITTED(HttpStatus.CONFLICT, "제출한 뒤에 리포트를 볼 수 있습니다."),
    CONCEPT_REQUIRED(HttpStatus.BAD_REQUEST, "연관 개념을 골라야 저장할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
