package com.vibelog.domain.quiz.model.policy;

import java.math.BigDecimal;

/*
 * [LessonQuizScoreChecker] (quiz 영역이 아직 없어 lesson 영역이 추가함, 08 문서 "안 멈추는 규칙 1-1")
 * 역할 : lesson 영역이 수업 화면(API-LSN-01)에서 퀴즈 활동의 최고 점수를 물을 때 쓰는 계약 (계약 ⑤).
 * 책임 : docs/design/api/quiz.md "영역 간 계약" 표 그대로. 진짜 구현은 QuizContractService (#84).
 */
public interface LessonQuizScoreChecker {

    /* 제출한 풀이 중 가장 높은 correct_rate (0~100). 없으면 null (PL-LSN-007) */
    BigDecimal getBestCorrectRate(Long memberId, Long activityId);
}
