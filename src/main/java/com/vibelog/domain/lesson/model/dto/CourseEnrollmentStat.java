package com.vibelog.domain.lesson.model.dto;

/*
 * 기간 내 수강 시작한 수강을 코스 하나로 묶은 집계 (LessonReader.getEnrollmentStatsByCourse, 운영자 대시보드
 * API-ADM-01). startedCount 는 그 기간에 시작한 수강 수, completedCount 는 그중 완료한 수강 수다.
 * 완료율 계산은 admin 영역(CompletionRatePolicy)이 한다.
 */
public record CourseEnrollmentStat(Long courseId, long startedCount, long completedCount) {
}
