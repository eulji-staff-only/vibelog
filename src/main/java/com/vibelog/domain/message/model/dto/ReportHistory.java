package com.vibelog.domain.message.model.dto;

/* 한 회원의 콘텐츠가 받은 신고 처리 결과 건수 (A-13 작성자 이력, A-08 신고 이력, FR-USR-009) */
public record ReportHistory(long pending, long hidden, long rejected) {
}
