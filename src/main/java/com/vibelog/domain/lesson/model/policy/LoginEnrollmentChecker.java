package com.vibelog.domain.lesson.model.policy;

/*
 * [LoginEnrollmentChecker] 계약 ⑫ (auth 가 부름)
 * 역할 : 이 회원에게 진행 중인 수강이 있는지 알려 준다. 로그인 도착지 ④(있으면 /my/learning)를 정할 때 쓴다 (API-USR-05·07).
 * 책임 : completed_at·dropped_at 이 모두 비어 있는 수강이 있으면 true. 명세: docs/design/api/lesson.md "영역 간 계약"
 */
public interface LoginEnrollmentChecker {

    boolean hasActiveEnrollment(Long memberId);
}
