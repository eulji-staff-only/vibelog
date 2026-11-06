package com.vibelog.domain.auth.model.entity;

/* 회원 상태 (member.status). 탈퇴 회원은 로그인할 수 없다 */
public enum MemberStatus {
    ACTIVE,
    WITHDRAWN
}
