package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.Portfolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
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

    /* U-35 같은 코스 작품 (API-PRT-10): 그 코스의 공개·삭제 표시 없는 작품, 영구 정지 회원 것과 신고로 숨긴 작품 제외. 정렬은 Pageable 로 */
    Page<Portfolio> findByCourseIdAndIsPublicTrueAndDeletedAtIsNullAndMemberIdNotInAndPortfolioIdNotIn(Long courseId,
            Collection<Long> hiddenMemberIds, Collection<Long> hiddenPortfolioIds, Pageable pageable);

    /* U-19 내 포트폴리오 (삭제 표시 제외, 최근 제출 먼저) */
    List<Portfolio> findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescPortfolioIdDesc(Long memberId);

    List<Portfolio> findAllByMemberIdAndIsPublicTrueAndDeletedAtIsNullOrderByCreatedAtDescPortfolioIdDesc(Long memberId);

    /* 회원들의 포트폴리오 ID, 삭제 표시한 것도 포함 (community 리뷰 요청 목록에서 영구 정지 회원 작품을 미리 뺀다, #215) */
    @Query("select p.portfolioId from Portfolio p where p.memberId in :memberIds")
    List<Long> findIdsByMemberIdIn(@Param("memberIds") Collection<Long> memberIds);

    /* 리뷰 요청(community #182)을 같은 회원끼리 차례로 처리하려고 그 회원의 포트폴리오 행을 쓰기 잠금으로 읽는다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portfolio p where p.memberId = :memberId and p.deletedAt is null")
    List<Portfolio> findLiveByMemberIdForUpdate(@Param("memberId") Long memberId);
}
