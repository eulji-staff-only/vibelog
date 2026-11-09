package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.DiagnosisQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisQuestionRepository extends JpaRepository<DiagnosisQuestion, Long> {

    /* 진단 설문에 보일 문항: 사용 중인 것만, 순서대로 (API-DGN-01) */
    List<DiagnosisQuestion> findByIsActiveTrueOrderBySortOrderAsc();
}
