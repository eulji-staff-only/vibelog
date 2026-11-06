package com.vibelog.domain.lesson.model.policy;

import org.springframework.stereotype.Component;

/*
 * [FakeLoginEnrollmentChecker]
 * 역할 : 계약 ⑫ LoginEnrollmentChecker 의 임시 구현. auth(#12)가 lesson 기반(#85)을 기다리지 않으려고 추가했다 (08 문서 규칙 1-2).
 * 책임 : 항상 "진행 중인 수강 없음"을 돌려준다. 진짜 구현이 생기기 전까지 로그인하면 지금처럼 /courses 로 가게 하려는 값이다.
 *        lesson 담당이 진짜 구현을 만들 때 이 파일을 지운다. 안 지우면 Bean 이 2개라 build 가 실패한다 (규칙 1-3).
 */
@Component
public class FakeLoginEnrollmentChecker implements LoginEnrollmentChecker {

    @Override
    public boolean hasActiveEnrollment(Long memberId) {
        return false;
    }
}
