package com.vibelog.domain.quiz.model.dto;

import java.math.BigDecimal;

/*
 * 퀴즈 화면(U-14, API-LSN-03) 틀 데이터: 퀴즈 제목, 문항 수, 최고 점수(PL-LSN-007, 제출한 풀이가 없으면 null).
 * 문항은 화면이 풀이 시작(API-LSN-04)을 불러 받는다.
 */
public record QuizPageView(Long courseId, Long lessonId, Long activityId, String title, int questionCount,
                           BigDecimal bestCorrectRate) {
}
