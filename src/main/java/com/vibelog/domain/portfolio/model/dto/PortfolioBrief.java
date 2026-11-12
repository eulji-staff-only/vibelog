package com.vibelog.domain.portfolio.model.dto;

/*
 * 다른 영역이 쓰는 포트폴리오 요약 (PortfolioReader, community 동료 리뷰 #182·#183).
 * assignmentId 는 코스 작품의 포트폴리오 과제 ID (평가 기준을 찾는다). 개인 작품이거나 과제가 없으면 null.
 */
public record PortfolioBrief(Long portfolioId, Long memberId, String title, String summary, Long courseId, Long assignmentId,
                             boolean isPublic, boolean deleted) {
}
