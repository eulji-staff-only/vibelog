package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.PortfolioAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.Collection;
import java.util.List;

import java.util.Collection;
import java.util.List;

public interface PortfolioAssignmentRepository extends JpaRepository<PortfolioAssignment, Long> {

    /* 코스의 과제. 코스당 1개 (uk_portfolio_assignment_course_id). 아직 안 만들었으면 비어 있다 (A-07) */
    Optional<PortfolioAssignment> findByCourseId(Long courseId);

    /* 이 코스에 포트폴리오 과제가 있나 (계약 ⑨) */
    boolean existsByCourseId(Long courseId);

    /* 코스 작품의 과제 (PortfolioReader → community 동료 리뷰 평가 기준) */
    List<PortfolioAssignment> findByCourseIdIn(Collection<Long> courseIds);
}
