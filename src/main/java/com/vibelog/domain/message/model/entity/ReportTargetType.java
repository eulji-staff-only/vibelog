package com.vibelog.domain.message.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * [ReportTargetType]
 * 역할 : 신고 대상의 종류 (report.target_type, ck_report_target_type, PL-RPT-001). label 은 A-13·HIDDEN 알림 문구.
 */
@Getter
@RequiredArgsConstructor
public enum ReportTargetType {
    POST("게시글"), PORTFOLIO("포트폴리오"), COMMENT("댓글"), REVIEW("리뷰"), MESSAGE("쪽지");

    private final String label;
}
