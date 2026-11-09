package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuizChoiceRepository extends JpaRepository<QuizChoice, Long> {

    /* 여러 문항의 보기를 한 번에, 순서대로 (풀이 시작 API-LSN-04) */
    List<QuizChoice> findByQuizQuestionIdInOrderBySortOrderAsc(Collection<Long> quizQuestionIds);

    /* 한 문항의 보기 (문항 확인 API-LSN-05: 고른 보기가 이 문항 것인지, 정답이 무엇인지) */
    List<QuizChoice> findByQuizQuestionId(Long quizQuestionId);
}
