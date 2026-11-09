package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /* 삭제 표시 없는 포트폴리오가 있나 (계약 ⑧) */
    boolean existsByMemberIdAndCourseIdAndDeletedAtIsNull(Long memberId, Long courseId);

    /* 그 코스에 제출한 내 포트폴리오 (삭제 표시 제외). U-17 "제출한 내 포트폴리오" (API-PRT-01) */
    Optional<Portfolio> findFirstByMemberIdAndCourseIdAndDeletedAtIsNull(Long memberId, Long courseId);

    /* 그 코스의 내 포트폴리오, 삭제 표시한 것도 포함 (uk_portfolio_member_course 라 최대 1개). 다시 제출하면 이 행을 되살린다 (API-PRT-04) */
    Optional<Portfolio> findByMemberIdAndCourseId(Long memberId, Long courseId);

    /* 회원의 삭제 표시 없는 포트폴리오 (계약 ⑩) */
    List<Portfolio> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    long countByMemberIdAndDeletedAtIsNull(Long memberId);

    /* U-19 내 포트폴리오 (삭제 표시 제외, 최근 제출 먼저) */
    List<Portfolio> findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescPortfolioIdDesc(Long memberId);

    List<Portfolio> findAllByMemberIdAndIsPublicTrueAndDeletedAtIsNullOrderByCreatedAtDescPortfolioIdDesc(Long memberId);
}
