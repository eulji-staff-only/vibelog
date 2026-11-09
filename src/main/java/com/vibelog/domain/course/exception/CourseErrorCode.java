package com.vibelog.domain.course.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [CourseErrorCode]
 * 역할 : course 영역(진단·트랙·코스)의 예외 종류.
 * 책임 : API 명세 7장 공통 에러 코드와 이름을 맞춘다 (docs/design/api/_template.md).
 *        지금은 이름·ID 로 찾았는데 없을 때만 둔다. 관리 기능의 코드(DUPLICATE_NAME, TRACK_HAS_COURSES,
 *        CATEGORY_HAS_COURSES, COURSE_NOT_PUBLISHABLE)는 쓰는 이슈(#95·#96·#117)에서 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "트랙을 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
    /* 진단 제출(API-DGN-02): 문항 수와 답 수가 다르거나, 문항에 없는 보기를 보냈을 때. 문구는 GlobalExceptionHandler 와 같다 */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요.");

    private final HttpStatus status;
    private final String message;
}
