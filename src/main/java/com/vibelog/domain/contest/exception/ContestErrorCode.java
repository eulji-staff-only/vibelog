package com.vibelog.domain.contest.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [ContestErrorCode]
 * 역할 : 콘테스트(contest) 영역의 비즈니스 예외 종류.
 * 책임 : 코드·상태·메시지는 API 명세 _template.md 7-3 그대로 둔다 (contest.md, #174).
 */
@Getter
@RequiredArgsConstructor
public enum ContestErrorCode implements ErrorCode {
    CONTEST_NOT_FOUND(HttpStatus.NOT_FOUND, "콘테스트를 찾을 수 없습니다."),
    CONTEST_MONTH_EXISTS(HttpStatus.CONFLICT, "그 달의 콘테스트가 이미 있습니다."),
    CONTEST_STARTED(HttpStatus.CONFLICT, "출품이 시작된 콘테스트는 월과 기간을 바꿀 수 없습니다."),
    ENTRY_CLOSED(HttpStatus.CONFLICT, "지금은 출품 기간이 아닙니다."),
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다."),
    PRIVATE_PORTFOLIO(HttpStatus.FORBIDDEN, "비공개된 작품입니다."),
    ALREADY_ENTERED(HttpStatus.CONFLICT, "이번 달 콘테스트에는 이미 출품했습니다."),
    VOTE_CLOSED(HttpStatus.CONFLICT, "지금은 투표 기간이 아닙니다."),
    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "출품작을 찾을 수 없습니다."),
    SELF_VOTE(HttpStatus.BAD_REQUEST, "내 작품에는 투표할 수 없습니다."),
    DAILY_VOTE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "투표는 하루 1번 할 수 있습니다. 내일 다시 투표해 주세요."),
    ALREADY_VOTED(HttpStatus.CONFLICT, "이미 투표한 작품입니다.");

    private final HttpStatus status;
    private final String message;
}
