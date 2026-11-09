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
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
    /* 운영자 수업 순서(API-ADM-18, #100): 폼 칸 검증이 아니라 보낸 수업 순서 목록 자체가 그 코스의 수업과 안 맞을 때 */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요."),
    /*
     * 명세(_template.md 7장)상 방식이 "③ 다른 화면으로 이동 + 토스트"라서 GlobalExceptionHandler(① 에러 화면)로
     * 던지지 않는다. 메시지만 이 enum 에서 가져오고, 실제 판단·리다이렉트는 LessonPageService·LessonActivityService가 한다.
     */
    LESSON_LOCKED(HttpStatus.FORBIDDEN, "이전 수업의 모의고사를 보고 리포트를 확인하면 열립니다."),
    ACTIVITY_LOCKED(HttpStatus.FORBIDDEN, "앞 퀴즈를 정답률 50% 이상으로 통과하면 열립니다.");

    private final HttpStatus status;
    private final String message;
}
