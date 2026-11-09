package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    /* 활동(퀴즈)별 사용 중인 문항 수 (FR-LSN-014, PL-LSN-003, #118 / 퀴즈 화면 문항 수 API-LSN-03) */
    int countByActivityIdAndIsActiveTrue(Long activityId);

    /* 퀴즈 풀이에 나오는 문항: 사용 중인 것만, 순서대로 (API-LSN-04·06) */
    List<QuizQuestion> findByActivityIdAndIsActiveTrueOrderBySortOrderAsc(Long activityId);
}
