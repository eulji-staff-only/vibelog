package com.vibelog.domain.portfolio.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [PortfolioErrorCode]
 * 역할 : portfolio 영역의 에러 목록. 상태·문구는 API 명세 _template.md 7-3 그대로 둔다.
 * 책임 : CommonErrorCode 에 있는 공통 에러(NOT_OWNER 등)는 여기 넣지 않는다.
 *        INVALID_INPUT 은 CommonErrorCode 에 없어 여기 둔다 (폼 Controller 가 칸별 메시지로 바꾼다).
 */
@Getter
@RequiredArgsConstructor
public enum PortfolioErrorCode implements ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다."),
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다."),
    PORTFOLIO_LOCKED(HttpStatus.FORBIDDEN, "마지막 수업의 오답 분석 리포트를 확인하면 열립니다."),
    ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 포트폴리오가 있습니다. 내 포트폴리오에서 수정해 주세요."),
    PRIVATE_PORTFOLIO(HttpStatus.FORBIDDEN, "비공개된 작품입니다."),
    PORTFOLIO_NOT_SUBMITTED(HttpStatus.FORBIDDEN, "포트폴리오를 제출하면 같은 코스 작품을 볼 수 있습니다."),
    /* 평가 에이전트는 포트폴리오를 제출한 뒤에 열린다 (#404). 설정 파일 주소를 직접 열었을 때 */
    AGENT_NOT_OPEN(HttpStatus.FORBIDDEN, "포트폴리오를 제출하면 평가 에이전트를 볼 수 있습니다. 제출한 뒤 내 포트폴리오에서 수정을 눌러 확인해 주세요."),
    HIDDEN_CONTENT(HttpStatus.FORBIDDEN, "운영 정책에 따라 숨겨진 콘텐츠입니다."),
    CONTEST_ENTERED(HttpStatus.CONFLICT, "콘테스트에 출품한 작품은 수정할 수 없습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
