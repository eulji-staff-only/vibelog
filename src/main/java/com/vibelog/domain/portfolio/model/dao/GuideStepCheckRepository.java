package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.GuideStepCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GuideStepCheckRepository extends JpaRepository<GuideStepCheck, Long> {

    /* 회원이 체크한 스텝 중 주어진 스텝들 (U-17 스텝 목록·"n / 전체 완료", API-PRT-01·02) */
    List<GuideStepCheck> findByMemberIdAndGuideStepIdIn(Long memberId, Collection<Long> guideStepIds);

    boolean existsByMemberIdAndGuideStepId(Long memberId, Long guideStepId);

    /* 체크를 풀면 행을 지운다 (DB 설계서 5-1, API-PRT-02) */
    void deleteByMemberIdAndGuideStepId(Long memberId, Long guideStepId);

    /* 이 스텝을 체크한 학습자가 있나. 있으면 스텝을 지우지 않고 끈다 (API-ADM-35) */
    boolean existsByGuideStepId(Long guideStepId);
}
