package com.vibelog.domain.message.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * [ReportReason]
 * 역할 : 신고 사유 (report.reason, ck_report_reason, PL-RPT-003). ETC 면 reason_detail 이 있어야 한다 (ck_report_reason_detail).
 */
@Getter
@RequiredArgsConstructor
public enum ReportReason {
    SPAM("스팸"), ABUSE("욕설/비방"), PLAGIARISM("도용"), PRIVACY("개인정보 노출"), ETC("기타");

    private final String label;
}
