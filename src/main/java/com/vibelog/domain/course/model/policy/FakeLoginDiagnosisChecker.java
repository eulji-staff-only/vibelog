package com.vibelog.domain.course.model.policy;

import org.springframework.stereotype.Component;

/*
 * [FakeLoginDiagnosisChecker]
 * 역할 : 계약 ⑫ LoginDiagnosisChecker 의 임시 구현. auth(#12)가 course 기반(#83)을 기다리지 않으려고 추가했다 (08 문서 규칙 1-2).
 * 책임 : 항상 "진단 기록 있음"을 돌려준다. 진짜 구현이 생기기 전까지 로그인하면 지금처럼 /courses 로 가게 하려는 값이다.
 *        course 담당이 진짜 구현을 만들 때 이 파일을 지운다. 안 지우면 Bean 이 2개라 build 가 실패한다 (규칙 1-3).
 */
@Component
public class FakeLoginDiagnosisChecker implements LoginDiagnosisChecker {

    @Override
    public boolean hasDiagnosisRecord(Long memberId) {
        return true;
    }
}
