package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.Portfolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /* 삭제 표시 없는 포트폴리오가 있나 (계약 ⑧) */
    boolean existsByMemberIdAndCourseIdAndDeletedAtIsNull(Long memberId, Long courseId);

    /* 회원의 삭제 표시 없는 포트폴리오 (계약 ⑩) */
    List<Portfolio> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    long countByMemberIdAndDeletedAtIsNull(Long memberId);

    /* 리뷰 요청(community #182)을 같은 회원끼리 차례로 처리하려고 그 회원의 포트폴리오 행을 쓰기 잠금으로 읽는다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portfolio p where p.memberId = :memberId and p.deletedAt is null")
    List<Portfolio> findLiveByMemberIdForUpdate(@Param("memberId") Long memberId);
}
