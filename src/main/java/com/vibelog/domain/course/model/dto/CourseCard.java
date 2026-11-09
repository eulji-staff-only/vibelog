package com.vibelog.domain.course.model.dto;

/* 코스 카드 한 장 (API-CRS-01·02): 코스명, 한 줄 소개, 트랙·카테고리 이름, 플랜 일수, 공개 수업 수 */
public record CourseCard(Long courseId, Long trackId, String title, String summary, String trackName,
                         String categoryName, int planDays, int lessonCount) {
}
