package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.ReviewRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/* 열린 요청 = closed_at 없음 + requested_at 이 since(지금 - 14일) 보다 뒤 (PL-REV-002) */
public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {

    Optional<ReviewRequest> findFirstByPortfolioIdAndClosedAtIsNullAndRequestedAtAfterOrderByReviewRequestIdDesc(Long portfolioId,
                                                                                                             LocalDateTime since);

    long countByPortfolioIdInAndClosedAtIsNullAndRequestedAtAfter(Collection<Long> portfolioIds, LocalDateTime since);

    List<ReviewRequest> findByPortfolioIdInAndClosedAtIsNullAndRequestedAtAfter(Collection<Long> portfolioIds, LocalDateTime since);

    /*
     * U-31 열린 요청 카드, 최근 요청순. hiddenPortfolioIds(영구 정지 회원 작품, PL-RPT-006)의 요청은 뺀다.
     * 빈 묶음이면 IN () 이 되지 않게 부르는 쪽이 0 하나를 넣는다.
     */
    Page<ReviewRequest> findByClosedAtIsNullAndRequestedAtAfterAndPortfolioIdNotInOrderByRequestedAtDescReviewRequestIdDesc(
            LocalDateTime since, Collection<Long> hiddenPortfolioIds, Pageable pageable);

    List<ReviewRequest> findByPortfolioId(Long portfolioId);
}
