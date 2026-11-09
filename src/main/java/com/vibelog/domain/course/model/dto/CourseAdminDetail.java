package com.vibelog.domain.course.model.dto;

import com.vibelog.domain.lesson.model.dto.CourseLessonRow;

import java.util.List;

/*
 * 운영자 코스 상세 (A-05, API-ADM-14): 코스 정보, 수업 목록(순서, 공개 상태), 포트폴리오 과제 상태.
 * 수업은 LessonReader, 공개 수업·과제가 있는지는 계약 ⑨(CourseLessonInfo.hasPublicLesson·CourseAssignmentChecker)로 받는다.
 * publishable 이 거짓이면 공개 버튼을 막고 이유를 보여 준다 (FR-CRS-006, #117).
 */
public record CourseAdminDetail(Long courseId, String title, boolean isPublic, String trackName, String categoryName,
                                List<CourseLessonRow> lessons, boolean hasAssignment, boolean hasPublicLesson) {

    /* 공개해도 되는가: 공개된 수업과 포트폴리오 과제가 모두 있어야 한다 (FR-CRS-006) */
    public boolean publishable() {
        return hasPublicLesson && hasAssignment;
    }
}
