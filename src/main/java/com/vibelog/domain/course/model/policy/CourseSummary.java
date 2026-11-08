package com.vibelog.domain.course.model.policy;

/*
 * [CourseSummary] CourseReader.getSummary 의 결과값.
 * 역할 : 코스 카드(내 학습 U-12 등)에 보여줄 트랙 이름·코스 이름을 담는다.
 */
public record CourseSummary(String trackName, String courseName) {
}
