package com.vibelog.domain.course.model.dto;

import com.vibelog.domain.lesson.model.policy.LessonSummary;

import java.util.List;

/*
 * 코스 상세 화면(U-11, API-CRS-03) 데이터.
 * - lessons: 공개 수업 목록, 수업 순서대로 (계약 ⑪ getPublicLessons). 공개 수업은 모두 모의고사가 있다.
 * - enrollmentStatus: NONE · ENROLLED · COMPLETED (계약 ⑪ getEnrollmentStatus). 비회원은 NONE.
 */
public record CourseDetailView(Long courseId, String title, Long trackId, String trackName, String categoryName,
                               String summary, String target, String goal, int planDays, String description,
                               List<LessonSummary> lessons, String enrollmentStatus) {

    public int lessonCount() {
        return lessons.size();
    }

    /* 수강 중이거나 완료한 코스 → "내 학습으로 가기" */
    public boolean isEnrolled() {
        return !"NONE".equals(enrollmentStatus);
    }
}
