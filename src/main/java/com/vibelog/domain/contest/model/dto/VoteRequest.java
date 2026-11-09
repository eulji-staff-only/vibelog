package com.vibelog.domain.contest.model.dto;

import jakarta.validation.constraints.NotNull;

/* API-CTS-03 투표 요청 json */
public record VoteRequest(@NotNull(message = "출품작을 골라 주세요") Long contestEntryId) {
}
