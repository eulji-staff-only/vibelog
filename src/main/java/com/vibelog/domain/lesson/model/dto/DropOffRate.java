package com.vibelog.domain.lesson.model.dto;

/*
 * 수업 하나의 이탈 집계 (LessonReader.getDropOffRateByLesson, 운영자 확장 대시보드 API-ADM-40, FR-ADM-003).
 * eligibleCount 는 그 기간에 열린(lesson_progress.unlocked_at 있음) 수업 진도 수, dropOffCount 는 그중 완료하지 못한(completed_at 없음) 수다.
 * 진행 중인 학습자도 포함하므로 "아직 못 끝낸 비율"이다. 비율 계산은 admin 영역이 한다.
 */
public record DropOffRate(int eligibleCount, int dropOffCount) {
}
