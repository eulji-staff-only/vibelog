package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    /* 이 풀이에서 이미 확인한 문항인지 (답 고정, PL-LSN-006) */
    boolean existsByQuizAttemptIdAndQuizQuestionId(Long quizAttemptId, Long quizQuestionId);

    /* 이 풀이에서 확인한 답 전부 (제출·채점 API-LSN-06) */
    List<QuizAnswer> findByQuizAttemptId(Long quizAttemptId);

    /* 이 문항에 풀이 기록(확인한 답)이 있는지. 있으면 고치지 않고 새 문항으로 저장한다 (API-ADM-27, DB 설계서 5-4) */
    boolean existsByQuizQuestionId(Long quizQuestionId);

    /*
     * 기간 내 제출한 풀이(submitted_at 기준)의 답을 문항의 개념 태그별로 센 것: [conceptTagId, 답한 수, 틀린 수].
     * from·to 가 null 이면 그 쪽 제한 없음 (admin 확장 대시보드 개념별 오답률, FR-ADM-003)
     */
    @Query("select q.conceptTagId, count(a), sum(case when a.isCorrect = false then 1 else 0 end) "
            + "from QuizAnswer a, QuizAttempt t, QuizQuestion q "
            + "where a.quizAttemptId = t.quizAttemptId and a.quizQuestionId = q.quizQuestionId and t.submittedAt is not null "
            + "and (:from is null or t.submittedAt >= :from) and (:to is null or t.submittedAt <= :to) "
            + "group by q.conceptTagId")
    List<Object[]> countByConceptTagBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /* 회원이 틀린 답 전부, 최근에 확인한 순 (틀린 문제 모아보기 API-LSN-10, #190). 풀이(quiz_attempt)의 회원으로 거른다 */
    @Query("select a from QuizAnswer a, QuizAttempt t where a.quizAttemptId = t.quizAttemptId "
            + "and t.memberId = :memberId and a.isCorrect = false order by a.checkedAt desc, a.quizAnswerId desc")
    List<QuizAnswer> findWrongAnswersOf(@Param("memberId") Long memberId);
}
