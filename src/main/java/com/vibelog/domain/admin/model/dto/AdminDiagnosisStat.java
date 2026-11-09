package com.vibelog.domain.admin.model.dto;

import com.vibelog.domain.admin.model.policy.DiagnosisRatioPolicy;

/*
 * 확장 대시보드(API-ADM-40)의 진단 완료·건너뛰기 현황. 개수는 조회 기간의 diagnosis_result 건수이고,
 * 비율(%)은 DiagnosisRatioPolicy 로 구한다. 결과가 없으면 비율은 둘 다 0이다.
 */
public record AdminDiagnosisStat(int completedCount, int skippedCount, int completedRate, int skippedRate) {

    public static AdminDiagnosisStat of(int completedCount, int skippedCount) {
        return new AdminDiagnosisStat(completedCount, skippedCount,
                DiagnosisRatioPolicy.completedRate(completedCount, skippedCount),
                DiagnosisRatioPolicy.skippedRate(completedCount, skippedCount));
    }
}
