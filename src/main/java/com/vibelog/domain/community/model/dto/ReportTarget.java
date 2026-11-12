package com.vibelog.domain.community.model.dto;

/*
 * 신고 대상 하나 (message 신고 API-RPT-01·02가 community Reader 로 읽는 값, P3).
 * authorMemberId 작성자, preview 목록용 미리보기(제목 또는 내용 앞 50자), content 원문.
 */
public record ReportTarget(Long authorMemberId, String preview, String content) {

    static final int PREVIEW_LENGTH = 50;

    /* 제목이 없는 글(댓글·리뷰)은 내용 앞 50자를 미리보기로 쓴다 */
    public static ReportTarget ofText(Long authorMemberId, String content) {
        String preview = content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) + "…" : content;
        return new ReportTarget(authorMemberId, preview, content);
    }
}
