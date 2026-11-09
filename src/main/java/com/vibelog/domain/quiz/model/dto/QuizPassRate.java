package com.vibelog.domain.quiz.model.dto;

/*
 * 퀴즈 통과율 집계 (QuizReader.getPassRate, 운영자 확장 대시보드 API-ADM-40, FR-ADM-003).
 * (회원, 퀴즈 활동) 한 쌍을 한 번으로 센다. attemptedCount 는 응시한 쌍 수, passedCount 는 그중 최고 점수가 통과 기준 이상인 쌍 수다.
 */
public record QuizPassRate(int attemptedCount, int passedCount) {
}
