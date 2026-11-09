package com.vibelog.domain.admin.model.dto;

import java.util.List;

/*
 * 확장 대시보드(API-ADM-40, FR-ADM-003) 2차 지표: 추천 트랙 vs 실제 수강, 수업별 이탈률, 퀴즈 통과율·개념별 오답률,
 * 수업별 모의고사 평균·약한 개념. 모두 조회 기간(from~to)의 값이고, 없으면 빈 목록이거나 0이다.
 */
public record AdminLearningAnalytics(List<TrackRecommendation> trackRecommendations, List<LessonDropOff> lessonDropOffs,
                                     QuizPassStat quizPass, List<ConceptWrongRate> conceptWrongRates,
                                     List<ExamLessonStat> examLessons) {

    /* 추천받은 트랙 하나: 추천받은 회원 수, 그중 그 트랙의 코스를 수강 시작한 회원 수, 비율(%) */
    public record TrackRecommendation(Long trackId, String trackName, int recommendedCount, int enrolledCount, int enrolledRate) {
    }

    /* 수업 하나: 기간에 열린 수 / 그중 못 끝낸 수 / 비율(%) */
    public record LessonDropOff(Long lessonId, String courseTitle, String lessonTitle, int eligibleCount, int dropOffCount,
                                int dropOffRate) {
    }

    /* 퀴즈 통과율: 응시한 (회원, 퀴즈) 수 / 통과한 수 / 비율(%) */
    public record QuizPassStat(int attemptedCount, int passedCount, int passRate) {
    }

    /* 개념 태그 하나: 답한 수 / 틀린 수 / 오답률(%) / 약한 개념(정답률 60% 미만) 여부 */
    public record ConceptWrongRate(Long conceptTagId, String conceptName, int questionCount, int wrongCount, int wrongRate,
                                   boolean weak) {
    }

    /* 모의고사를 본 수업 하나: 평균 점수(소수 첫째 자리)와 약한 개념 이름들 */
    public record ExamLessonStat(Long lessonId, String courseTitle, String lessonTitle, double averageScore,
                                 List<String> weakConcepts) {
    }
}
