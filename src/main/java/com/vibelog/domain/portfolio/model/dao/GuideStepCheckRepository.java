package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.GuideStepCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideStepCheckRepository extends JpaRepository<GuideStepCheck, Long> {

    /* 이 스텝을 체크한 학습자가 있나. 있으면 스텝을 지우지 않고 끈다 (API-ADM-35) */
    boolean existsByGuideStepId(Long guideStepId);
}
