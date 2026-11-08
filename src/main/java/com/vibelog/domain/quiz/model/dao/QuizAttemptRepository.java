package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /* 회원이 제출한 풀이, 최근 제출 순 (QuizReader.getSubmittedResultsOf) */
    List<QuizAttempt> findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(Long memberId);
}
