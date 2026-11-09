package com.vibelog.domain.course.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [CourseErrorCode]
 * 역할 : course 영역(진단·트랙·코스)의 예외 종류.
 * 책임 : API 명세 7장 공통 에러 코드와 이름을 맞춘다 (docs/design/api/_template.md).
 *        코스 관리·공개의 코드(COURSE_NOT_PUBLISHABLE 등)는 쓰는 이슈(#96·#117)에서 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "트랙을 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
    /* 진단 제출(API-DGN-02): 문항 수와 답 수가 다르거나, 문항에 없는 보기를 보냈을 때. 문구는 GlobalExceptionHandler 와 같다 */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요."),
    /* 트랙·카테고리 관리 (API-ADM-03~09, #95). 문구는 명세 7장 그대로 */
    DUPLICATE_NAME(HttpStatus.CONFLICT, "같은 이름이 이미 있습니다."),
    TRACK_HAS_COURSES(HttpStatus.CONFLICT, "코스가 있어 삭제할 수 없습니다. 코스를 먼저 옮겨 주세요."),
    CATEGORY_HAS_COURSES(HttpStatus.CONFLICT, "코스가 있어 삭제할 수 없습니다. 코스를 먼저 옮겨 주세요."),
    /* 명세 7장에 따로 없는 코드: 없는 카테고리(TRACK_NOT_FOUND 와 같은 방식), 트랙 순서 중복(DB uk_track_sort_order, 폼 칸 메시지) */
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    SORT_ORDER_TAKEN(HttpStatus.CONFLICT, "다른 트랙이 이미 쓰는 순서입니다.");

    private final HttpStatus status;
    private final String message;
}
