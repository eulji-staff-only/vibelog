package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    /* 이 풀이에서 이미 확인한 문항인지 (답 고정, PL-LSN-006) */
    boolean existsByQuizAttemptIdAndQuizQuestionId(Long quizAttemptId, Long quizQuestionId);

    /* 이 풀이에서 확인한 답 전부 (제출·채점 API-LSN-06) */
    List<QuizAnswer> findByQuizAttemptId(Long quizAttemptId);
}
