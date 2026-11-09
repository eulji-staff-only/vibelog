package com.vibelog.domain.portfolio.model.dto;

import java.time.LocalDateTime;

/*
 * [MyPortfolioCard]
 * 역할 : U-19 내 포트폴리오 카드 한 장 (API-PRT-05).
 * 책임 : 제목, 코스, 한 줄 요약, 공개 여부, 제출일. 코스 이름은 course 영역 CourseReader 로 읽은 값이다.
 *        courseId 가 null 이면 개인 작품(P2)이라 "수정"(U-17 수정 상태) 링크를 그리지 않는다.
 */
public record MyPortfolioCard(Long portfolioId, Long courseId, String courseTitle, String title, String summary,
                              boolean isPublic, LocalDateTime submittedAt) {
}
