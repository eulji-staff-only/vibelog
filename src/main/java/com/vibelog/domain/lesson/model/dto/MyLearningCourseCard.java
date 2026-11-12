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
    /*
     * "학습하기"가 보낼 수업 (설계서 U-12 "학습하기 → U-13").
     * 진행 중인 수업이 있으면 그 수업, 없으면 열린 수업 중 마지막 수업(이미 끝낸 수업 다시 보기), 열린 수업이 없으면 null.
     * null 이면 화면이 코스 상세로 보낸다.
     */
    public Long learningLessonId() {
        return lessons.stream()
                .filter(lesson -> lesson.status() == LessonStatus.IN_PROGRESS)
                .map(MyLearningLessonCard::lessonId)
                .findFirst()
                .orElseGet(() -> lessons.stream()
                        .filter(lesson -> !lesson.status().isLocked())
                        .map(MyLearningLessonCard::lessonId)
                        .reduce((first, second) -> second)
                        .orElse(null));
    }
}
