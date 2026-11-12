package com.vibelog.domain.course.model.entity;

/* 진단 결과 상태 (diagnosis_result.status). 진단을 마쳤거나(COMPLETED) 건너뛰었다(SKIPPED). FR-DGN-001·003 */
public enum DiagnosisStatus {
    COMPLETED,
    SKIPPED
}
