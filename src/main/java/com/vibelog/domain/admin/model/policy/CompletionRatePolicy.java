package com.vibelog.domain.admin.model.policy;

/*
 * [CompletionRatePolicy]
 * 역할 : 대시보드(A-01, API-ADM-01)의 완료율 계산 규칙 (FR-ADM-002).
 * 책임 : 분모는 그 기간에 시작한 수강 수, 분자는 그중 완료한 수강 수다. 시작한 수강이 없으면 0%로 본다
 *        (나누기 0 방지). 결과는 정수 % 로 반올림한다.
 */
public final class CompletionRatePolicy {

    private CompletionRatePolicy() {
    }

    public static int calculate(long startedCount, long completedCount) {
        if (startedCount <= 0) {
            return 0;
        }
        return (int) Math.round(completedCount * 100.0 / startedCount);
    }
}
