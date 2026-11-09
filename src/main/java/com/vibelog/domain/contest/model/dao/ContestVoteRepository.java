package com.vibelog.domain.contest.model.dao;

import com.vibelog.domain.contest.model.entity.ContestVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestVoteRepository extends JpaRepository<ContestVote, Long> {
}
