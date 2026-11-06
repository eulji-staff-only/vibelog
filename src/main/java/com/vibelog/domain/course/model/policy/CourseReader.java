package com.vibelog.domain.course.model.policy;

/*
 * [CourseReader] (course 영역이 아직 없어 lesson 영역이 추가함, 08 문서 "안 멈추는 규칙 1-2")
 * 역할 : 코스가 존재하고 공개 상태인지 알려 준다.
 * 책임 : 수강 시작(API-ENR-01)에서 COURSE_NOT_FOUND(없음·비공개) 판단에 쓴다.
 *        course 영역(#83)이 구현되면 이 인터페이스의 진짜 구현으로 바꾸고 FakeCourseReader 를 지운다.
 */
public interface CourseReader {

    boolean isPublicCourse(Long courseId);
}
