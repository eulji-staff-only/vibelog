package com.vibelog.domain.lesson.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [LessonErrorCode]
 * 역할 : lesson 영역(수강·수업·활동)의 예외 종류.
 * 책임 : API 명세 7장 공통 에러 코드(LESSON_NOT_FOUND)와 이름을 맞추고,
 *        명세에 없는 영역 간 계약 전용 코드(ACTIVITY_NOT_FOUND, ENROLLMENT_NOT_FOUND)는
 *        없는 ID로 계약을 부를 때를 대비해 lesson 영역이 추가한다 (docs/design/api/lesson.md 26줄).
 */
@Getter
@RequiredArgsConstructor
public enum LessonErrorCode implements ErrorCode {

    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "수업을 찾을 수 없습니다."),
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "활동을 찾을 수 없습니다."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 중인 코스를 찾을 수 없습니다."),
    LESSON_PROGRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "수업 진도를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
