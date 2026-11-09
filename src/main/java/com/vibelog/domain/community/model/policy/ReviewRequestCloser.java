package com.vibelog.domain.community.model.policy;

/*
 * [ReviewRequestCloser] 계약 ⑭ (portfolio → community)
 * 역할 : 포트폴리오를 비공개하거나 삭제할 때 그 작품의 열린 리뷰 요청을 닫는다 (FR-REV-005, PL-REV-002).
 * 책임 : community 영역이 구현한다 (ReviewContractService). 부르는 쪽은 portfolio 영역 공개 전환·삭제.
 */
public interface ReviewRequestCloser {

    void closeOpenRequests(Long portfolioId);
}
