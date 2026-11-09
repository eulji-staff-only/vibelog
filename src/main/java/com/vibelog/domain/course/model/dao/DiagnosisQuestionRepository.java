package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.DiagnosisQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisQuestionRepository extends JpaRepository<DiagnosisQuestion, Long> {

    /* 진단 설문에 보일 문항: 사용 중인 것만, 순서대로 (API-DGN-01) */
    List<DiagnosisQuestion> findByIsActiveTrueOrderBySortOrderAsc();

    /* 진단 설정(A-09, API-ADM-41): 사용 중지 포함 전체, 순서대로 (#187) */
    List<DiagnosisQuestion> findAllByOrderBySortOrderAscDiagnosisQuestionIdAsc();

    /* 사용 중인 문항 수. 마지막 사용 중 문항은 중지할 수 없다 (API-ADM-44) */
    long countByIsActiveTrue();
}
