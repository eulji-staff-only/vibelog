package com.vibelog.domain.contest.model.dto;

/* API-CTS-03 투표 응답 json {"contestEntryId", "votedToday": true} */
public record VoteResponse(Long contestEntryId, boolean votedToday) {
}
