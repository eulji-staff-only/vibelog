package com.vibelog.domain.admin.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [AdminErrorCode]
 * 역할 : admin 영역의 에러 목록. 상태·문구는 API 명세 admin.md·_template.md 7-2 그대로 둔다.
 * 책임 : 공통 에러(NOT_OWNER 등)는 여기 넣지 않는다. 대시보드 기간 필터(API-ADM-01, FR-ADM-002)처럼
 *        admin 화면에서만 쓰는 검증 실패를 담는다.
 */
@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "시작일이 종료일보다 늦습니다. 기간을 다시 확인해 주세요.");

    private final HttpStatus status;
    private final String message;
}
