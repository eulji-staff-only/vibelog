package com.vibelog.domain.contest.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/*
 * [ContestVote]
 * 역할 : 투표 1건 (contest_vote, FR-CTS-003).
 * 책임 : 하루 1표(uk_contest_vote_daily)와 같은 작품 1번(uk_contest_vote_entry_voter)은 DB 제약이 지킨다 (PL-CTS-003).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContestVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contestVoteId;

    private Long contestId;

    private Long contestEntryId;

    private Long voterMemberId;

    private LocalDate voteDate;

    private ContestVote(Long contestId, Long contestEntryId, Long voterMemberId, LocalDate voteDate) {
        this.contestId = contestId;
        this.contestEntryId = contestEntryId;
        this.voterMemberId = voterMemberId;
        this.voteDate = voteDate;
    }

    public static ContestVote create(Long contestId, Long contestEntryId, Long voterMemberId, LocalDate voteDate) {
        return new ContestVote(contestId, contestEntryId, voterMemberId, voteDate);
    }
}
