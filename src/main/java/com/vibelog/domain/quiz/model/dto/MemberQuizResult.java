package com.vibelog.domain.quiz.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* 회원이 제출한 퀴즈 풀이 한 번 (QuizReader.getSubmittedResultsOf, 운영자 회원 상세 API-ADM-38) */
public record MemberQuizResult(Long activityId, BigDecimal correctRate, boolean passed, LocalDateTime submittedAt) {
}
