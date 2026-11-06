package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /* 삭제 표시 없는 포트폴리오가 있나 (계약 ⑧) */
    boolean existsByMemberIdAndCourseIdAndDeletedAtIsNull(Long memberId, Long courseId);

    /* 회원의 삭제 표시 없는 포트폴리오 (계약 ⑩) */
    List<Portfolio> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    long countByMemberIdAndDeletedAtIsNull(Long memberId);
}
