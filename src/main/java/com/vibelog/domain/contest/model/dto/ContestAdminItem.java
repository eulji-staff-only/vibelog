package com.vibelog.domain.contest.model.dto;

import java.time.LocalDateTime;

/* A-11 콘테스트 목록 한 줄 (월, 주제, 출품·투표 기간, 출품 수, 발표 여부) */
public record ContestAdminItem(Long contestId, String contestMonth, String topic, LocalDateTime entryStartsAt,
                               LocalDateTime entryEndsAt, LocalDateTime voteStartsAt, LocalDateTime voteEndsAt,
                               long entryCount, boolean announced) {
}
