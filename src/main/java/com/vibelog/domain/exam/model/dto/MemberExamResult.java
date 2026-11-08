package com.vibelog.domain.exam.model.dto;

import java.time.LocalDateTime;

/* 회원이 제출한 모의고사 한 번 (ExamReader.getSubmittedResultsOf, 운영자 회원 상세 API-ADM-38) */
public record MemberExamResult(Long lessonId, int attemptNo, int score, LocalDateTime submittedAt) {
}
