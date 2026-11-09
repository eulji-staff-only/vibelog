package com.vibelog.domain.admin.model.policy;

/*
 * [DiagnosisRatioPolicy]
 * 역할 : 확장 대시보드(A-01, API-ADM-40, FR-ADM-003)의 진단 완료·건너뛰기 비율 계산 규칙.
 * 책임 : 분모는 기간 내 진단 결과 전체(완료 + 건너뜀)다. 결과가 없으면 둘 다 0%로 본다(나누기 0 방지).
 *        완료 비율은 정수 %로 반올림하고, 건너뛰기 비율은 100에서 뺀 값이라 두 비율의 합은 항상 100%다.
 */
public final class DiagnosisRatioPolicy {

    private DiagnosisRatioPolicy() {
    }

    public static int completedRate(int completedCount, int skippedCount) {
        int total = completedCount + skippedCount;
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(completedCount * 100.0 / total);
    }

    public static int skippedRate(int completedCount, int skippedCount) {
        if (completedCount + skippedCount <= 0) {
            return 0;
        }
        return 100 - completedRate(completedCount, skippedCount);
    }
}
