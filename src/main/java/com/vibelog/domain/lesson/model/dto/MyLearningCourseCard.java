package com.vibelog.domain.lesson.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * [MyLearningCourseCard]
 * 역할 : 내 학습(U-12, API-ENR-02) 코스 카드 하나의 응답값.
 * 책임 : progressPercent 는 PL-ENR-006 그대로 "리포트까지 확인한 수업 수 ÷ 공개 수업 수" 를 저장하지 않고 계산한 값이다.
 */
public record MyLearningCourseCard(
        Long courseId,
        String trackName,
        String courseName,
        int progressPercent,
        String nextStepText,
        List<MyLearningLessonCard> lessons,
        PortfolioStatus portfolioStatus,
        List<String> remainingConditions,
        boolean completed,
        LocalDateTime completedAt
) {
}
