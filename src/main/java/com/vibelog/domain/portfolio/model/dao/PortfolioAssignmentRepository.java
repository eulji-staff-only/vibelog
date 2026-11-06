package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.PortfolioAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioAssignmentRepository extends JpaRepository<PortfolioAssignment, Long> {

    /* 이 코스에 포트폴리오 과제가 있나 (계약 ⑨) */
    boolean existsByCourseId(Long courseId);
}
