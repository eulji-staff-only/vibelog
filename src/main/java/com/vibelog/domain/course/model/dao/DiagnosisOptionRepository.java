package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.DiagnosisOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DiagnosisOptionRepository extends JpaRepository<DiagnosisOption, Long> {

    /* 여러 문항의 보기를 한 번에 가져온다 (문항마다 조회하지 않으려고) */
    List<DiagnosisOption> findByDiagnosisQuestionIdInOrderBySortOrderAsc(Collection<Long> diagnosisQuestionIds);
}
