package com.vibelog.domain.contest.model.dto;

import com.vibelog.domain.portfolio.model.dto.PortfolioBrief;

import java.time.LocalDateTime;
import java.util.List;

/*
 * U-32 이달의 콘테스트 (API-CTS-01). 단계(entryOpen·voteOpen·voteEnded)에 맞는 보드만 채운다.
 * 출품 단계: myEntry(내 출품, 없으면 null) 또는 myPortfolios(출품할 내 공개 포트폴리오).
 * 투표 단계: entries(출품순, 표 수 없음), votedToday, emailVerified.
 */
public record ContestCurrentView(Long contestId, String contestMonth, String topic, String description,
                                 LocalDateTime entryStartsAt, LocalDateTime entryEndsAt,
                                 LocalDateTime voteStartsAt, LocalDateTime voteEndsAt,
                                 boolean entryOpen, boolean voteOpen, boolean voteEnded,
                                 MyEntry myEntry, List<PortfolioBrief> myPortfolios,
                                 List<EntryCard> entries, boolean votedToday, boolean emailVerified) {

    /* 내 출품. 작품을 비공개·삭제하면 취소된다 (PL-CTS-006) */
    public record MyEntry(Long portfolioId, String title, boolean canceled) {
    }

    /* 투표 보드 출품작 카드. mine = 내 작품(투표 불가), voted = 내가 투표함 */
    public record EntryCard(Long contestEntryId, Long portfolioId, String title, String summary, String nickname,
                            boolean mine, boolean voted) {
    }
}
