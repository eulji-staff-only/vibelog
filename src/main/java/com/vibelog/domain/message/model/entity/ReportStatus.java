package com.vibelog.domain.message.model.entity;

/*
 * [ReportStatus]
 * 역할 : 신고 처리 상태 (report.status, ck_report_status, PL-RPT-004). PENDING 처리 대기, HIDDEN 숨김, REJECTED 기각.
 */
public enum ReportStatus {
    PENDING, HIDDEN, REJECTED
}
