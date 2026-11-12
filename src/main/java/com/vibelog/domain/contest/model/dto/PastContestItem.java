package com.vibelog.domain.contest.model.dto;

/*
 * U-33 지난 콘테스트 목록 한 줄 (API-CTS-04). firstPlace 는 1등 작품 제목(공동 1등은 ", "로 잇고, 비공개·삭제는 "비공개된 작품").
 * 수상작이 없으면 null → "수상작 없음", 시상 보류 중이면(held) "발표 보류" (contest 명세 결정 17).
 */
public record PastContestItem(Long contestId, String contestMonth, String topic, String firstPlace, boolean held) {
}
