package com.vibelog.domain.lesson.model.policy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/*
 * [CourseLessonInfo]
 * 역할 : course 영역이 코스의 공개 수업 여부·수강 상태·공개 수업 목록·수를 물을 때 쓰는 계약 (계약 ⑨⑪).
 * 책임 : lesson 영역(domain/lesson)이 구현한다. docs/design/api/lesson.md "영역 간 계약" 그대로.
 */
public interface CourseLessonInfo {

    boolean hasPublicLesson(Long courseId);

    String getEnrollmentStatus(Long memberId, Long courseId);

    List<LessonSummary> getPublicLessons(Long courseId);

    Map<Long, Integer> countPublicLessons(Collection<Long> courseIds);
}
