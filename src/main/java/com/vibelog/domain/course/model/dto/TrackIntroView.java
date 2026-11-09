package com.vibelog.domain.course.model.dto;

import java.util.List;

/*
 * 트랙 소개 화면(U-09, API-CRS-01) 데이터.
 * stage 는 트랙 순서(sort_order)다. 트랙 순서가 Starter → Controller → Builder → Explorer 단계다 (PL-DGN-002).
 */
public record TrackIntroView(Long trackId, String name, int stage, String target, String description,
                             List<CourseCard> courses) {

    public CourseCard firstCourse() {
        return courses.isEmpty() ? null : courses.get(0);
    }
}
