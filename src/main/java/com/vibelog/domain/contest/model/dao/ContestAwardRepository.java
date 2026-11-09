package com.vibelog.domain.contest.model.dao;

import com.vibelog.domain.contest.model.entity.ContestAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContestAwardRepository extends JpaRepository<ContestAward, Long> {

    /* 회원이 받은 배지: [contestAwardId, contestMonth, awardRank] 최근 월부터 */
    @Query("select a.contestAwardId, c.contestMonth, a.awardRank from ContestAward a"
            + " join ContestEntry e on e.contestEntryId = a.contestEntryId join Contest c on c.contestId = a.contestId"
            + " where e.memberId = :memberId order by c.contestMonth desc, a.awardRank asc")
    List<Object[]> findBadgeRowsOf(@Param("memberId") Long memberId);
}
