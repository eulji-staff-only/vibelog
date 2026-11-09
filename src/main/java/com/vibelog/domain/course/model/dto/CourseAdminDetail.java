package com.vibelog.domain.course.model.dto;

import com.vibelog.domain.lesson.model.dto.CourseLessonRow;

import java.util.List;

/*
 * 운영자 코스 상세 (A-05, API-ADM-14): 코스 정보, 수업 목록(순서, 공개 상태), 포트폴리오 과제 상태.
 * 수업은 LessonReader, 과제가 있는지는 계약 ⑨(CourseAssignmentChecker)로 받는다.
 */
public record CourseAdminDetail(Long courseId, String title, boolean isPublic, String trackName, String categoryName,
                                List<CourseLessonRow> lessons, boolean hasAssignment) {
}
