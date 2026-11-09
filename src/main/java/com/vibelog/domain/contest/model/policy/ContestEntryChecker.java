package com.vibelog.domain.contest.model.policy;

/*
 * [ContestEntryChecker] 계약 ⑮ (portfolio → contest)
 * 역할 : 이 포트폴리오가 콘테스트에 출품 중인가. portfolio 수정(API-PRT-06)이 CONTEST_ENTERED 로 막는다 (PL-PRT-007).
 * 책임 : 출품 중 = 취소되지 않은 출품이 있고 그 콘테스트의 투표가 아직 안 끝남 (contest 명세 결정 1). contest 영역이 구현한다.
 */
public interface ContestEntryChecker {

    boolean isEntered(Long portfolioId);
}
