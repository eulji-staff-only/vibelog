package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.PortfolioFaq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioFaqRepository extends JpaRepository<PortfolioFaq, Long> {

    /* 과제의 FAQ 순서대로 (U-17 4번 영역, A-07 FAQ 탭) */
    List<PortfolioFaq> findByPortfolioAssignmentIdOrderBySortOrderAsc(Long portfolioAssignmentId);
}
