package com.vibelog.domain.lesson.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [LessonErrorCode]
 * 역할 : lesson 영역(수강·수업·활동)의 예외 종류.
 * 책임 : API 명세 7장 공통 에러 코드(LESSON_NOT_FOUND)와 이름을 맞추고,
 *        명세에 없는 영역 간 계약 전용 코드(ACTIVITY_NOT_FOUND)는
 *        없는 ID로 계약을 부를 때를 대비해 lesson 영역이 추가한다 (docs/design/api/lesson.md 26줄).
 *        ENROLLMENT_NOT_FOUND 는 수강 포기(API-ENR-04, P2)의 명세 에러 코드이고, 계약 호출에도 같이 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum LessonErrorCode implements ErrorCode {

    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "수업을 찾을 수 없습니다."),
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "활동을 찾을 수 없습니다."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 중인 코스가 아닙니다."),
    LESSON_PROGRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "수업 진도를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
    /* 운영자 수업 순서(API-ADM-18, #100): 폼 칸 검증이 아니라 보낸 수업 순서 목록 자체가 그 코스의 수업과 안 맞을 때 */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요."),
    /* 운영자 활동 순서(API-ADM-24, #101): 개념 뒤에 퀴즈가 하나씩 오지 않는 순서로 저장했을 때 (PL-LSN-001) */
    INVALID_ORDER(HttpStatus.BAD_REQUEST, "개념 뒤에는 퀴즈가 하나씩 와야 합니다."),
    /*
     * 운영자 수업 공개(API-ADM-33, FR-LSN-014, #118): 개념 뒤 퀴즈 순서·퀴즈 3~5문항·정답·모의고사 문항 중
     * 빠진 항목이 있을 때 (PL-LSN-003). 어떤 항목이 빠졌는지는 A-06 편집 화면의 체크리스트(LessonPublishStatus)로
     * 보여주고, 여기 메시지는 토스트에 쓸 안내만 담는다 (ErrorCode는 인스턴스마다 다른 문구를 담지 않는다).
     */
    LESSON_NOT_PUBLISHABLE(HttpStatus.CONFLICT, "필수 항목이 비어 있어 공개할 수 없습니다. 편집 화면에서 빠진 항목을 확인해주세요."),
    /*
     * 명세(_template.md 7장)상 방식이 "③ 다른 화면으로 이동 + 토스트"라서 GlobalExceptionHandler(① 에러 화면)로
     * 던지지 않는다. 메시지만 이 enum 에서 가져오고, 실제 판단·리다이렉트는 LessonPageService·LessonActivityService가 한다.
     */
    LESSON_LOCKED(HttpStatus.FORBIDDEN, "이전 수업의 모의고사를 보고 리포트를 확인하면 열립니다."),
    ACTIVITY_LOCKED(HttpStatus.FORBIDDEN, "앞 퀴즈를 정답률 50% 이상으로 통과하면 열립니다.");

    private final HttpStatus status;
    private final String message;
}
