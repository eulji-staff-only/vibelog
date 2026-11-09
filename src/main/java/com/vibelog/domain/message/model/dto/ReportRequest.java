package com.vibelog.domain.message.model.dto;

import com.vibelog.domain.message.model.entity.ReportReason;
import com.vibelog.domain.message.model.entity.ReportTargetType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* API-RPT-01 신고 요청 json (M-11). 사유가 "기타"면 내용 1~500자 (PL-RPT-003, ck_report_reason_detail) */
public record ReportRequest(
        @NotNull(message = "신고할 콘텐츠를 다시 골라 주세요") ReportTargetType targetType,
        @NotNull(message = "신고할 콘텐츠를 다시 골라 주세요") Long targetId,
        @NotNull(message = "신고 사유를 골라 주세요") ReportReason reason,
        @Size(max = 500, message = ReportRequest.DETAIL_RULE) String reasonDetail) {

    static final String DETAIL_RULE = "기타 사유는 내용을 1~500자로 써 주세요";

    @AssertTrue(message = DETAIL_RULE)
    public boolean isDetailWrittenForEtc() {
        return reason != ReportReason.ETC || (reasonDetail != null && !reasonDetail.isBlank());
    }

    /* 저장할 기타 내용: "기타"일 때만 앞뒤 공백을 뺀 값, 아니면 없음 */
    public String detailToSave() {
        return reason == ReportReason.ETC ? reasonDetail.strip() : null;
    }
}
