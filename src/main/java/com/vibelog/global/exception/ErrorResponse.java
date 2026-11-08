package com.vibelog.global.exception;

import jakarta.servlet.http.HttpServletRequest;

/*
 * [ErrorResponse]
 * 역할 : /api/** 요청이 실패했을 때 보내는 JSON 본문 (D-08, API 명세 7-1 ⑤). 예) {"code": "NOT_OWNER", "message": "본인만 할 수 있습니다."}
 * 책임 : - code 는 에러 코드 이름(ErrorCode.name()), message 는 화면에 그대로 보여 줄 문구다. 상태 코드는 HTTP 응답에 담는다.
 *        - 성공 응답은 감싸지 않고 명세에 적힌 값을 그대로 담은 DTO 를 돌려준다.
 */
public record ErrorResponse(String code, String message) {

    public static boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }
}
