package com.vibelog.domain.contest.model.dao;

import com.vibelog.domain.contest.model.entity.ContestAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContestAwardRepository extends JpaRepository<ContestAward, Long> {

    /* 회원이 받은 배지: [contestAwardId, contestMonth, awardRank] 최근 월부터 */
    @Query("select a.contestAwardId, c.contestMonth, a.awardRank from ContestAward a"
            + " join ContestEntry e on e.contestEntryId = a.contestEntryId join Contest c on c.contestId = a.contestId"
            + " where e.memberId = :memberId order by c.contestMonth desc, a.awardRank asc")
    List<Object[]> findBadgeRowsOf(@Param("memberId") Long memberId);

    /* 발표가 끝난 콘테스트 (A-11 발표 여부) */
    @Query("select distinct a.contestId from ContestAward a where a.contestId in :contestIds")
    List<Long> findAnnouncedIn(@Param("contestIds") Collection<Long> contestIds);

    /* 수상작, 순위순 (같은 순위는 먼저 출품한 것부터) */
    List<ContestAward> findByContestIdOrderByAwardRankAscContestEntryIdAsc(Long contestId);

    /* U-33 목록의 1등 작품 */
    List<ContestAward> findByContestIdInAndAwardRankOrderByContestEntryIdAsc(Collection<Long> contestIds, int awardRank);
}
