package com.vibelog.domain.contest.model.dao;

import com.vibelog.domain.contest.model.entity.Contest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContestRepository extends JpaRepository<Contest, Long> {

    /* 이번 달 콘테스트 (contest_month = yyyy-MM, uk_contest_contest_month) */
    Optional<Contest> findByContestMonth(String contestMonth);

    boolean existsByContestMonth(String contestMonth);

    /* A-11 목록, 최근 월부터 */
    Page<Contest> findPageByOrderByContestMonthDesc(Pageable pageable);

    /* 발표할 콘테스트: 투표가 끝난 지 하루가 안 됐고 수상 기록이 없음 (contest 명세 "결과 발표") */
    @Query("select c from Contest c where c.voteEndsAt <= :now and c.voteEndsAt > :since"
            + " and not exists (select a from ContestAward a where a.contestId = c.contestId)")
    List<Contest> findToAnnounce(@Param("now") LocalDateTime now, @Param("since") LocalDateTime since);

    /* U-33 지난 콘테스트: 투표가 끝난 것, 최근 월부터 */
    Page<Contest> findByVoteEndsAtLessThanEqualOrderByContestMonthDesc(LocalDateTime now, Pageable pageable);
}
