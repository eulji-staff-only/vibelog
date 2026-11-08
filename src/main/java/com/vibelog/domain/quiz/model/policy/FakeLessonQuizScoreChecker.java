package com.vibelog.domain.quiz.model.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/*
 * [FakeLessonQuizScoreChecker]
 * 역할 : quiz 영역이 구현되기 전까지 쓰는 임시 LessonQuizScoreChecker (08 문서 "안 멈추는 규칙 1-1").
 * 책임 : quiz_attempt 가 아직 없어, 모든 활동을 "제출한 풀이 없음"(null)으로 본다.
 *        quiz 영역(#84)이 생기면 quiz 담당자가 진짜 구현으로 바꾸고 이 클래스는 지운다.
 */
@Component
public class FakeLessonQuizScoreChecker implements LessonQuizScoreChecker {

    @Override
    public BigDecimal getBestCorrectRate(Long memberId, Long activityId) {
        return null;
    }
}
