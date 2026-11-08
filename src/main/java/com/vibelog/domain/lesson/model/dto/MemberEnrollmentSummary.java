package com.vibelog.domain.lesson.model.dto;

import java.time.LocalDateTime;

/*
 * 회원의 수강 하나 (LessonReader.getEnrollmentsOf, 운영자 회원 상세 API-ADM-38).
 * progressPercent 는 리포트까지 확인한 수업 수 ÷ 공개 수업 수 (PL-ENR-006). 코스 이름은 course 영역 값이라 담지 않는다.
 */
public record MemberEnrollmentSummary(Long courseId, LocalDateTime startedAt, int progressPercent,
                                      boolean completed, boolean dropped) {
}
