package com.vibelog.domain.quiz.model.dto;

/*
 * 개념 태그 하나의 퀴즈 문항 풀이 집계 (QuizReader.getWrongRateByConceptTag, 운영자 확장 대시보드 API-ADM-40, FR-ADM-003).
 * questionCount 는 제출된 풀이에서 그 태그 문항에 답한 횟수, wrongCount 는 그중 틀린 횟수다. 오답률·약한 개념 판정은 admin 영역이 한다.
 */
public record ConceptWrongStat(int questionCount, int wrongCount) {
}
