package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.ReviewCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewCriterionRepository extends JpaRepository<ReviewCriterion, Long> {

    /* 과제의 쓰는 기준, sort_order 순 */
    @Query("select c from ReviewCriterion c where c.portfolioAssignmentId = :assignmentId and c.isActive = true"
            + " order by c.sortOrder asc, c.reviewCriterionId asc")
    List<ReviewCriterion> findActiveOf(@Param("assignmentId") Long assignmentId);

    /* 받은 리뷰에 기준 이름을 붙일 때 (지금은 안 쓰는 기준도) */
    List<ReviewCriterion> findByReviewCriterionIdIn(Collection<Long> ids);
}
