package com.vibelog.domain.portfolio.model.policy;

/*
 * [MemberPortfolioCleaner] 계약 ⑩ (auth 가 부름)
 * 역할 : 탈퇴한 회원의 포트폴리오를 모두 삭제 표시한다 (FR-USR-008, API-USR-16, DB 설계서 5-3).
 * 책임 : 행을 지우지 않고 deleted_at 을 기록한다. 부르는 쪽 @Transactional 안에서 부른다 (08 문서 규칙 1-6).
 *        명세: docs/design/api/portfolio.md "영역 간 계약"
 */
public interface MemberPortfolioCleaner {

    void deleteAllOf(Long memberId);
}
