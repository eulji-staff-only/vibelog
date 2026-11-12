package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.GuideStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideStepRepository extends JpaRepository<GuideStep, Long> {

    /* 과제의 사용 중인 스텝, 순서대로 (A-07 편집 화면·저장) */
    List<GuideStep> findByPortfolioAssignmentIdAndIsActiveTrueOrderBySortOrderAsc(Long portfolioAssignmentId);
}
