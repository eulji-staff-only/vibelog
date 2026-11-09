package com.vibelog.domain.message.model.entity;

/*
 * [ReportReason]
 * 역할 : 신고 사유 (report.reason, ck_report_reason, PL-RPT-003). ETC 면 reason_detail 이 있어야 한다 (ck_report_reason_detail).
 */
public enum ReportReason {
    SPAM, ABUSE, PLAGIARISM, PRIVACY, ETC
}
