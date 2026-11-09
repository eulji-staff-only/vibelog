package com.vibelog.domain.contest.model.dao;

import com.vibelog.domain.contest.model.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContestRepository extends JpaRepository<Contest, Long> {

    /* 이번 달 콘테스트 (contest_month = yyyy-MM, uk_contest_contest_month) */
    Optional<Contest> findByContestMonth(String contestMonth);
}
