package com.vibelog.domain.portfolio.model.dto;

import org.springframework.data.domain.Page;

/*
 * [CoursePortfoliosView]
 * 역할 : U-35 같은 코스 작품 보기에 그릴 값 (API-PRT-10). 카드는 최근 제출순 20개씩 (page 는 화면에서 1부터).
 */
public record CoursePortfoliosView(Long courseId, String courseTitle, Page<CoursePortfolioCard> portfolios) {
}
