package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.DiagnosisResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisResultRepository extends JpaRepository<DiagnosisResult, Long> {

    /* 상태는 COMPLETED·SKIPPED 둘뿐이라(ck_diagnosis_result_status) 행이 있으면 진단 기록이 있다 */
    boolean existsByMemberId(Long memberId);
}
