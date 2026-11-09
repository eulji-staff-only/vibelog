package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    /* 활동(퀴즈)별 사용 중인 문항 수 (FR-LSN-014, PL-LSN-003, #118) */
    int countByActivityIdAndIsActiveTrue(Long activityId);
}
