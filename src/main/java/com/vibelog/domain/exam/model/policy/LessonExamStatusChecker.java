package com.vibelog.domain.exam.model.policy;

import java.util.Collection;
import java.util.Map;

/*
 * [LessonExamStatusChecker]
 * 역할 : 영역 간 계약 ④ — lesson 영역이 부르는 수업별 모의고사 상태 조회.
 * 책임 : docs/design/api/exam.md "영역 간 계약" 그대로. (API-ENR-02 "응시함", API-ENR-03 리포트 링크)
 */
public interface LessonExamStatusChecker {

    /* 수업별로 마지막에 제출한 응시의 attempt_no. 제출한 응시가 없는 수업은 빠진다. */
    Map<Long, Integer> getLatestSubmittedAttemptNos(Long memberId, Collection<Long> lessonIds);
}
