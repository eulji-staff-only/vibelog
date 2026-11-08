package com.vibelog.domain.contest.model.dao;

import com.vibelog.domain.contest.model.entity.ContestEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ContestEntryRepository extends JpaRepository<ContestEntry, Long> {

    /* 출품 중 (계약 ⑮⑯): 취소되지 않았고 그 콘테스트의 투표가 아직 안 끝난 출품 */
    @Query("select e from ContestEntry e join Contest c on c.contestId = e.contestId"
            + " where e.portfolioId = :portfolioId and e.canceledAt is null and c.voteEndsAt > :now")
    List<ContestEntry> findOngoingByPortfolioId(@Param("portfolioId") Long portfolioId, @Param("now") LocalDateTime now);

    /* A-11 출품 수 (취소 제외): [contestId, count] */
    @Query("select e.contestId, count(e) from ContestEntry e where e.contestId in :contestIds and e.canceledAt is null group by e.contestId")
    List<Object[]> countLiveByContestIds(@Param("contestIds") Collection<Long> contestIds);
}
