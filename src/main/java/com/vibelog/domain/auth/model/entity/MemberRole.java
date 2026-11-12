package com.vibelog.domain.auth.model.entity;

/* 회원 역할 (member.role). 비회원은 로그인하지 않은 상태라 값이 없다 (PL-USR-008) */
public enum MemberRole {
    LEARNER,
    ADMIN
}
