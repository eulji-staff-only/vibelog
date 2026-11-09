package com.vibelog.domain.contest.model.policy;

/*
 * [ContestEntryCanceler] 계약 ⑯ (portfolio → contest)
 * 역할 : 출품 중인 작품을 비공개·삭제하면 출품을 취소한다. 받은 표는 무효 (PL-CTS-006).
 * 책임 : portfolio 가 삭제(API-PRT-07)·비공개 전환(API-PRT-08) 때 부른다. 출품 중이 아니면 아무것도 하지 않는다.
 */
public interface ContestEntryCanceler {

    void cancelEntryOf(Long portfolioId);
}
