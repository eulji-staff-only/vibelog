package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    /* 이 풀이에서 이미 확인한 문항인지 (답 고정, PL-LSN-006) */
    boolean existsByQuizAttemptIdAndQuizQuestionId(Long quizAttemptId, Long quizQuestionId);

    /* 이 풀이에서 확인한 답 전부 (제출·채점 API-LSN-06) */
    List<QuizAnswer> findByQuizAttemptId(Long quizAttemptId);

    /* 이 문항에 풀이 기록(확인한 답)이 있는지. 있으면 고치지 않고 새 문항으로 저장한다 (API-ADM-27, DB 설계서 5-4) */
    boolean existsByQuizQuestionId(Long quizQuestionId);

    /* 회원이 틀린 답 전부, 최근에 확인한 순 (틀린 문제 모아보기 API-LSN-10, #190). 풀이(quiz_attempt)의 회원으로 거른다 */
    @Query("select a from QuizAnswer a, QuizAttempt t where a.quizAttemptId = t.quizAttemptId "
            + "and t.memberId = :memberId and a.isCorrect = false order by a.checkedAt desc, a.quizAnswerId desc")
    List<QuizAnswer> findWrongAnswersOf(@Param("memberId") Long memberId);
}
