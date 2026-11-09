package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    /* 활동(퀴즈)별 사용 중인 문항 수 (FR-LSN-014, PL-LSN-003, #118 / 퀴즈 화면 문항 수 API-LSN-03) */
    int countByActivityIdAndIsActiveTrue(Long activityId);

    /* 퀴즈 풀이에 나오는 문항: 사용 중인 것만, 순서대로 (API-LSN-04·06) */
    List<QuizQuestion> findByActivityIdAndIsActiveTrueOrderBySortOrderAsc(Long activityId);

    /* 운영자 A-06 문항 목록: 여러 퀴즈 활동의 문항 전부(중지 포함), 순서대로 (#102) */
    List<QuizQuestion> findByActivityIdInOrderBySortOrderAscQuizQuestionIdAsc(Collection<Long> activityIds);

    /* 새 문항은 그 활동 마지막 문항 뒤에 붙인다 (API-ADM-26) */
    Optional<QuizQuestion> findTopByActivityIdOrderBySortOrderDesc(Long activityId);
}
