package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /* 회원이 제출한 풀이, 최근 제출 순 (QuizReader.getSubmittedResultsOf) */
    List<QuizAttempt> findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(Long memberId);

    /* 회원이 그 퀴즈에서 제출한 풀이 중 정답률이 가장 높은 1건 (계약 ⑤ getBestCorrectRate) */
    Optional<QuizAttempt> findTopByMemberIdAndActivityIdAndSubmittedAtIsNotNullOrderByCorrectRateDesc(Long memberId, Long activityId);

    /* 이 퀴즈를 전에 풀어 본 적이 있는지 → 다시 풀면 문항·보기 순서를 섞는다 (PL-LSN-004·005) */
    boolean existsByMemberIdAndActivityId(Long memberId, Long activityId);
}
