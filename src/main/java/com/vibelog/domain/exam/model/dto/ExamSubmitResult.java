package com.vibelog.domain.exam.model.dto;

/*
 * 제출·채점(API-EXM-04) 결과. 리포트 주소에 쓸 회차를 돌려준다.
 * alreadySubmitted 면 이미 제출한 응시라 채점하지 않았다 (ATTEMPT_SUBMITTED, 화면 요청이라 ③ 리포트 + 토스트).
 */
public record ExamSubmitResult(int attemptNo, boolean alreadySubmitted) {
}
