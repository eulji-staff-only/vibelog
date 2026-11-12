package com.vibelog.domain.course.model.dto;

/* 운영자 코스 목록 한 줄 (A-04, API-ADM-11): 이름, 트랙, 카테고리, 공개 상태, 수강자 수 */
public record CourseAdminRow(Long courseId, String title, String trackName, String categoryName, boolean isPublic,
                             long enrollmentCount) {
}
