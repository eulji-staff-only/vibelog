package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ConceptRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConceptRelationRepository extends JpaRepository<ConceptRelation, Long> {

    /* 연관 개념 보기(API-LSN-07): 개념 하나의 연관 개념을 sort_order 순으로 */
    List<ConceptRelation> findBySourceActivityIdOrderBySortOrderAsc(Long sourceActivityId);
}
