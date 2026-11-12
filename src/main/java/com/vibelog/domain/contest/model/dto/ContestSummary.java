package com.vibelog.domain.contest.model.dto;

import java.time.LocalDateTime;

/* 이번 달 콘테스트 요약 (ContestReader.findCurrent): 메인 화면이 쓴다 (API-USR-01, #203) */
public record ContestSummary(Long contestId, String topic, LocalDateTime entryStartsAt, LocalDateTime entryEndsAt,
                             LocalDateTime voteStartsAt, LocalDateTime voteEndsAt) {
}
