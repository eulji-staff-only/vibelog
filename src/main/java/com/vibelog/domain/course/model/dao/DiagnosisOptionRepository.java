package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.DiagnosisOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DiagnosisOptionRepository extends JpaRepository<DiagnosisOption, Long> {

    /* 여러 문항의 보기를 한 번에 가져온다 (문항마다 조회하지 않으려고) */
    List<DiagnosisOption> findByDiagnosisQuestionIdInOrderBySortOrderAsc(Collection<Long> diagnosisQuestionIds);

    /* 이 트랙에 점수를 주는 보기가 있는지 (트랙 삭제 막기, fk_diagnosis_option_track_id, #95) */
    boolean existsByTrackId(Long trackId);

    /* 한 문항의 보기, 순서대로 (문항 수정 API-ADM-43, #187) */
    List<DiagnosisOption> findByDiagnosisQuestionIdOrderBySortOrderAsc(Long diagnosisQuestionId);
}
