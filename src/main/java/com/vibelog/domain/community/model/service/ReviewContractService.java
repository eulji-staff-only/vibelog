package com.vibelog.domain.community.model.service;

import com.vibelog.domain.community.model.dao.ReviewRequestRepository;
import com.vibelog.domain.community.model.entity.ReviewRequest;
import com.vibelog.domain.community.model.policy.ReviewRequestCloser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [ReviewContractService]
 * 역할 : 계약 ⑭ ReviewRequestCloser 구현. 포트폴리오를 비공개·삭제하면 그 작품의 리뷰 요청을 닫는다 (FR-REV-005).
 * 책임 : 아직 안 닫힌 요청에만 closed_at 을 남긴다. 받은 리뷰는 그대로 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewContractService implements ReviewRequestCloser {

    private final ReviewRequestRepository reviewRequestRepository;

    @Override
    public void closeOpenRequests(Long portfolioId) {
        reviewRequestRepository.findByPortfolioId(portfolioId).forEach(ReviewRequest::close);
    }
}
