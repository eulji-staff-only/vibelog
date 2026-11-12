package com.vibelog.domain.course.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [CourseErrorCode]
 * 역할 : course 영역(진단·트랙·코스)의 예외 종류.
 * 책임 : API 명세 7장 공통 에러 코드와 이름을 맞춘다 (docs/design/api/_template.md).
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
    /* 명세 7장에 따로 없는 코드: 없는 카테고리(TRACK_NOT_FOUND 와 같은 방식), 트랙 순서 중복(DB uk_track_sort_order, 폼 칸 메시지),
       진단 보기·결과가 쓰는 트랙 삭제(fk_diagnosis_option_track_id·fk_diagnosis_result_recommended_track_id) */
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    SORT_ORDER_TAKEN(HttpStatus.CONFLICT, "다른 트랙이 이미 쓰는 순서입니다."),
    TRACK_IN_USE(HttpStatus.CONFLICT, "진단 문항이나 진단 결과에 쓰이는 트랙이라 삭제할 수 없습니다."),
    /* 코스 공개 (API-ADM-16, FR-CRS-006). 공개된 수업이나 포트폴리오 과제가 없으면 막는다. 화면은 "상세 + 토스트"(③). 문구는 명세 7장 그대로 */
    COURSE_NOT_PUBLISHABLE(HttpStatus.CONFLICT, "공개된 수업과 포트폴리오 과제가 있어야 공개할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
