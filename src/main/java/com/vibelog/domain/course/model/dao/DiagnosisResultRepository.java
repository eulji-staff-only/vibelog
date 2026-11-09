package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.DiagnosisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiagnosisResultRepository extends JpaRepository<DiagnosisResult, Long> {

    /* 상태는 COMPLETED·SKIPPED 둘뿐이라(ck_diagnosis_result_status) 행이 있으면 진단 기록이 있다 */
    boolean existsByMemberId(Long memberId);

    /* 회원의 가장 최근 진단 결과 (코스 목록 추천 트랙, API-CRS-02). 진단 다시 하기(P2)로 여러 행이 생길 수 있다 */
    Optional<DiagnosisResult> findFirstByMemberIdOrderByDiagnosisResultIdDesc(Long memberId);

    /* 이 트랙을 추천받은 진단 결과가 있는지 (트랙 삭제 막기, fk_diagnosis_result_recommended_track_id, #95) */
    boolean existsByRecommendedTrackId(Long recommendedTrackId);

    /* 기간 내 진단 결과를 상태별로 센 것: [status, count] (created_at 기준). from·to 가 null 이면 그 쪽 제한 없음 (admin 확장 대시보드, FR-ADM-003) */
    @Query("select r.status, count(r) from DiagnosisResult r where (:from is null or r.createdAt >= :from)"
            + " and (:to is null or r.createdAt <= :to) group by r.status")
    List<Object[]> countByStatusBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
