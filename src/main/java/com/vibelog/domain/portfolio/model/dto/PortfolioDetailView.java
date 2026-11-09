package com.vibelog.domain.portfolio.model.dto;

import java.time.LocalDateTime;

/*
 * [PortfolioDetailView]
 * 역할 : U-20 포트폴리오 상세에 그릴 값 (API-PRT-09).
 * 책임 : 작성자 닉네임은 auth 영역 MemberReader, 코스 이름은 course 영역 CourseReader 로 읽은 값이다.
 *        mine 이면 "내 포트폴리오로"·"수정"을 그리고, 비공개면 "나만 볼 수 있습니다" 안내를 붙인다.
 */
public record PortfolioDetailView(Long portfolioId, String title, String resultUrl, String summary, String retrospective,
                                  String aiTools, String agentResult, String screenshotUrl,
                                  Long courseId, String courseTitle, String authorNickname,
                                  LocalDateTime submittedAt, boolean isPublic, boolean mine, boolean hiddenNotice) {
}
