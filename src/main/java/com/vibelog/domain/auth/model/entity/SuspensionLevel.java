package com.vibelog.domain.auth.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * [SuspensionLevel]
 * 역할 : 제재 단계 (member_suspension.level, PL-RPT-005). 경고 → 7일 정지 → 30일 정지 → 영구 정지.
 * 책임 : 경고는 기록만 하고 막지 않는다 (명세 auth.md P2 "정지 중"). days 가 0 이면 끝나는 날이 없다.
 */
@Getter
@RequiredArgsConstructor
public enum SuspensionLevel {
    WARNING("경고", 0),
    DAYS_7("7일 정지", 7),
    DAYS_30("30일 정지", 30),
    PERMANENT("영구 정지", 0);

    private final String label;
    private final int days;

    public boolean blocks() {
        return this != WARNING;
    }

    /* 운영자 모달의 기본 선택: 지난 제재의 다음 단계 */
    public SuspensionLevel next() {
        return this == PERMANENT ? PERMANENT : values()[ordinal() + 1];
    }
}
