package com.vibelog.domain.portfolio.model.dto;

/*
 * 신고 대상 하나 (message 신고 API-RPT-01·02가 portfolio Reader 로 읽는 값, P3).
 * authorMemberId 작성자, preview 목록용 미리보기(제목), content 원문.
 */
public record ReportTarget(Long authorMemberId, String preview, String content) {
}
