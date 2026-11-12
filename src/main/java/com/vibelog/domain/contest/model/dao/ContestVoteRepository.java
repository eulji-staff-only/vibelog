package com.vibelog.domain.contest.model.dao;

import com.vibelog.domain.contest.model.entity.ContestVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ContestVoteRepository extends JpaRepository<ContestVote, Long> {

    /* 오늘 이 콘테스트에 투표했나 (하루 1표, uk_contest_vote_daily) */
    boolean existsByContestIdAndVoterMemberIdAndVoteDate(Long contestId, Long voterMemberId, LocalDate voteDate);

    /* 이 출품작에 투표했나 (같은 작품 1번, uk_contest_vote_entry_voter) */
    boolean existsByContestEntryIdAndVoterMemberId(Long contestEntryId, Long voterMemberId);

    /* 내가 투표한 출품작 (U-32 "투표함" 표시) */
    @Query("select v.contestEntryId from ContestVote v where v.contestId = :contestId and v.voterMemberId = :memberId")
    List<Long> findEntryIdsVotedBy(@Param("contestId") Long contestId, @Param("memberId") Long memberId);

    /* 출품작별 받은 표 수: [contestEntryId, count] (집계·결과) */
    @Query("select v.contestEntryId, count(v) from ContestVote v where v.contestId = :contestId group by v.contestEntryId")
    List<Object[]> countByEntry(@Param("contestId") Long contestId);
}
