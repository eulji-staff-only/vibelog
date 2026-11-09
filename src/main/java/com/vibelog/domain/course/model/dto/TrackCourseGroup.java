package com.vibelog.domain.course.model.dto;

import java.util.List;

/* 코스 목록(U-10)에서 트랙 하나로 묶은 공개 코스 카드 */
public record TrackCourseGroup(Long trackId, String trackName, List<CourseCard> courses) {
}
