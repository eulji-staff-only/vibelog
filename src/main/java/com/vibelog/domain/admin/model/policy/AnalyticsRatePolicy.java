package com.vibelog.domain.admin.model.policy;

import com.vibelog.domain.quiz.model.policy.QuizGradingPolicy;

/*
 * [AnalyticsRatePolicy]
 * 역할 : 확장 대시보드(A-01, API-ADM-40, FR-ADM-003) 2차 지표의 비율 계산 규칙.
 * 책임 : - 비율(%)은 분자 ÷ 분모 × 100 을 정수로 반올림한다. 분모가 0이면 0%로 본다(나누기 0 방지).
 *        - 약한 개념은 정답률 60% 미만이다 (PL-LSN-008, QuizGradingPolicy.WEAK_CORRECT_RATE 와 같은 값).
 *          반올림한 비율이 아니라 개수로 비교해 경계(60%)가 정확하다.
 *        - DB 를 보지 않는 계산만 한다.
 */
public final class AnalyticsRatePolicy {

    private AnalyticsRatePolicy() {
    }

    public static int percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / total);
    }

    /* 틀린 수 wrongCount, 답한 수 questionCount 로 본 약한 개념 여부: 맞힌 수 × 100 < 60 × 답한 수 */
    public static boolean isWeakConcept(int wrongCount, int questionCount) {
        return questionCount > 0 && (long) (questionCount - wrongCount) * 100 < (long) QuizGradingPolicy.WEAK_CORRECT_RATE * questionCount;
    }
}
