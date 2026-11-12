package com.vibelog.domain.portfolio.model.policy;

/*
 * [LessonPortfolioChecker] 계약 ⑧ (lesson 이 부름)
 * 역할 : 이 회원이 이 코스의 포트폴리오를 제출했는지 알려 준다 (FR-ENR-002·004).
 * 책임 : 삭제 표시(deleted_at) 없는 포트폴리오가 있어야 "제출함"이다. 명세: docs/design/api/portfolio.md "영역 간 계약"
 */
public interface LessonPortfolioChecker {

    boolean hasSubmitted(Long memberId, Long courseId);
}
