package com.vibelog.domain.exam.model.dto;

/*
 * 수업별 모의고사에서 약한 개념(정답률 60% 미만, PL-LSN-008) 하나 (ExamReader.getWeakConceptsByLesson, 운영자 확장 대시보드 API-ADM-40).
 * conceptActivityId 는 문항의 연관 개념 활동 ID, correctCount 는 그 개념 문항을 맞힌 횟수, questionCount 는 답한 횟수다.
 */
public record ExamWeakConcept(Long conceptActivityId, int correctCount, int questionCount) {
}
