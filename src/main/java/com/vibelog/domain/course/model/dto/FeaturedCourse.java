package com.vibelog.domain.course.model.dto;

import com.vibelog.domain.course.model.entity.Course;

/* 메인 화면 대표 코스 카드 한 장 (U-01, API-USR-01): 코스명, 한 줄 소개, 학습 대상, 예상 기간(플랜 일수) */
public record FeaturedCourse(Long courseId, String title, String summary, String target, int planDays) {

    public static FeaturedCourse from(Course course) {
        return new FeaturedCourse(course.getCourseId(), course.getTitle(), course.getSummary(), course.getTarget(),
                course.getPlanDays());
    }
}
