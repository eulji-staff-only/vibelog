package com.vibelog.domain.course.model.policy;

/*
 * [LoginDiagnosisChecker] 계약 ⑫ (auth 가 부름)
 * 역할 : 이 회원에게 진단 기록이 있는지 알려 준다. 로그인 도착지 ③(없으면 /diagnosis)을 정할 때 쓴다 (API-USR-05·07).
 * 책임 : diagnosis_result(COMPLETED·SKIPPED)가 하나라도 있으면 true. 명세: docs/design/api/course.md "영역 간 계약"
 */
public interface LoginDiagnosisChecker {

    boolean hasDiagnosisRecord(Long memberId);
}
