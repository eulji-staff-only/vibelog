package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.PortfolioAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PortfolioAssignmentRepository extends JpaRepository<PortfolioAssignment, Long> {

    /* 이 코스에 포트폴리오 과제가 있나 (계약 ⑨) */
    boolean existsByCourseId(Long courseId);

    /* 코스 작품의 과제 (PortfolioReader → community 동료 리뷰 평가 기준) */
    List<PortfolioAssignment> findByCourseIdIn(Collection<Long> courseIds);
}
