package com.vibelog.domain.course.model.policy;

import org.springframework.stereotype.Component;

/*
 * [FakeCourseReader]
 * 역할 : course 영역이 구현되기 전까지 쓰는 임시 CourseReader (08 문서 "안 멈추는 규칙 1-2").
 * 책임 : sql/02-seed.sql 에 코스 샘플 데이터가 아직 없어, 모든 코스를 "존재하고 공개"로 보고
 *        트랙·코스 이름은 샘플 문구를 돌려준다.
 *        course 영역이 생기면 course 담당자가 진짜 CourseReader 구현으로 바꾸고 이 클래스는 지운다.
 */
@Component
public class FakeCourseReader implements CourseReader {

    @Override
    public boolean isPublicCourse(Long courseId) {
        return true;
    }

    @Override
    public CourseSummary getSummary(Long courseId) {
        return new CourseSummary("샘플 트랙", "샘플 코스 " + courseId);
    }
}
