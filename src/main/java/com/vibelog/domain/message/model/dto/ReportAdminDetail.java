package com.vibelog.domain.message.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * A-13 오른쪽: 한 대상의 원문·작성자 이력·처리 대기 신고 (API-RPT-02 reportId). pending 이면 숨김·기각 버튼을 보여 준다.
 */
public record ReportAdminDetail(Long reportId, String targetTypeLabel, String content, Long authorMemberId, String authorNickname,
                                ReportHistory authorHistory, List<Entry> reports, boolean pending) {

    /* 신고 한 건: 신고자, 사유, 기타 내용, 시각 */
    public record Entry(String reporterNickname, String reasonLabel, String reasonDetail, LocalDateTime createdAt) {
    }
}
