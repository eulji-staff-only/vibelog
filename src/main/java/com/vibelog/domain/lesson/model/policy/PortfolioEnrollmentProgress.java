package com.vibelog.domain.lesson.model.policy;

/*
 * [PortfolioEnrollmentProgress]
 * 역할 : portfolio 영역이 포트폴리오 접근 권한을 묻고, 제출·삭제를 알릴 때 쓰는 계약 (계약 ⑥⑦).
 * 책임 : lesson 영역(domain/lesson)이 구현한다. docs/design/api/lesson.md "영역 간 계약" 표 그대로.
 */
public interface PortfolioEnrollmentProgress {

    PortfolioAccess getAccess(Long memberId, Long courseId);

    void recordSubmitted(Long memberId, Long courseId);

    void recordDeleted(Long memberId, Long courseId);
}
