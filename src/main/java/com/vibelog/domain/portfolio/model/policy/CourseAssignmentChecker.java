package com.vibelog.domain.portfolio.model.policy;

/*
 * [CourseAssignmentChecker] 계약 ⑨ (course 가 부름)
 * 역할 : 이 코스에 포트폴리오 과제가 있는지 알려 준다. 코스 공개 조건에 쓴다 (FR-CRS-006).
 * 책임 : portfolio_assignment 행이 있으면 true. 명세: docs/design/api/portfolio.md "영역 간 계약"
 */
public interface CourseAssignmentChecker {

    boolean hasAssignment(Long courseId);
}
