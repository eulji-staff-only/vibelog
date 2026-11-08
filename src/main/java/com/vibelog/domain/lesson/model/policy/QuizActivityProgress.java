package com.vibelog.domain.lesson.model.policy;

/*
 * [QuizActivityProgress]
 * 역할 : quiz 영역이 퀴즈 활동의 잠김·통과 기준을 묻고, 통과를 기록할 때 쓰는 계약 (계약 ①).
 * 책임 : lesson 영역(domain/lesson)이 구현한다. docs/design/api/lesson.md "영역 간 계약" 표 그대로.
 */
public interface QuizActivityProgress {

    QuizActivityInfo getQuizActivity(Long memberId, Long activityId);

    NextStep recordQuizPassed(Long memberId, Long activityId);
}
