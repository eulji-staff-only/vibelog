package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /* 회원이 제출한 풀이, 최근 제출 순 (QuizReader.getSubmittedResultsOf) */
    List<QuizAttempt> findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(Long memberId);

    /* 회원이 그 퀴즈에서 제출한 풀이 중 정답률이 가장 높은 1건 (계약 ⑤ getBestCorrectRate) */
    Optional<QuizAttempt> findTopByMemberIdAndActivityIdAndSubmittedAtIsNotNullOrderByCorrectRateDesc(Long memberId, Long activityId);

    /* 기간 내 제출한 풀이 전체 (submitted_at 기준). from·to 가 null 이면 그 쪽 제한 없음 (admin 확장 대시보드 퀴즈 통과율, FR-ADM-003) */
    @Query("select t from QuizAttempt t where t.submittedAt is not null "
            + "and (:from is null or t.submittedAt >= :from) and (:to is null or t.submittedAt <= :to)")
    List<QuizAttempt> findSubmittedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /* 이 퀴즈를 전에 풀어 본 적이 있는지 → 다시 풀면 문항·보기 순서를 섞는다 (PL-LSN-004·005) */
    boolean existsByMemberIdAndActivityId(Long memberId, Long activityId);
}
