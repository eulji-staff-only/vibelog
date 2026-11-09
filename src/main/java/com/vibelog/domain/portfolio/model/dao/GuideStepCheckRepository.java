package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.GuideStepCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GuideStepCheckRepository extends JpaRepository<GuideStepCheck, Long> {

    /* 회원이 체크한 스텝 중 주어진 스텝들 (U-17 스텝 목록·"n / 전체 완료", API-PRT-01·02) */
    List<GuideStepCheck> findByMemberIdAndGuideStepIdIn(Long memberId, Collection<Long> guideStepIds);

    /*
     * 체크 저장 (API-PRT-02). 이미 있으면 그대로 둔다 (uk_guide_step_check_member_step).
     * 같은 체크가 동시에 와도 한쪽이 유니크 제약 오류(409)를 받지 않게 DB 가 한 번에 판단한다.
     * 중복 말고 다른 제약 위반(없는 회원·스텝)은 그대로 오류가 난다.
     */
    @Modifying
    @Query(value = "INSERT INTO guide_step_check (member_id, guide_step_id, checked_at) VALUES (:memberId, :guideStepId, NOW()) "
            + "ON DUPLICATE KEY UPDATE checked_at = checked_at", nativeQuery = true)
    void insertIfAbsent(@Param("memberId") Long memberId, @Param("guideStepId") Long guideStepId);

    /* 체크를 풀면 행을 지운다 (DB 설계서 5-1, API-PRT-02) */
    void deleteByMemberIdAndGuideStepId(Long memberId, Long guideStepId);

    /* 이 스텝을 체크한 학습자가 있나. 있으면 스텝을 지우지 않고 끈다 (API-ADM-35) */
    boolean existsByGuideStepId(Long guideStepId);
}
