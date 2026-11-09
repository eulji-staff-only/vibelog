package com.vibelog.domain.message.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * A-13 신고 목록의 한 줄 = 처리 대기 신고를 대상별로 묶은 것 (API-RPT-02).
 * reportId 는 그 대상의 가장 최근 신고(오른쪽에 열 때 쓴다). reasons 는 "스팸 2, 욕설/비방 1"처럼 사유별 건수 (사유 순서대로).
 */
public record ReportAdminItem(Long reportId, String targetTypeLabel, String preview, String authorNickname,
                              String reasons, List<String> reporterNicknames, int count, boolean autoHidden,
                              LocalDateTime lastReportedAt) {
}
