package com.vibelog.domain.course.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [DiagnosisResult]
 * 역할 : 진단 결과 테이블(diagnosis_result)과 매핑된다. 회원이 진단을 마치거나 건너뛰면 한 행이 생긴다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 diagnosis_result 그대로 둔다. 회원·추천 트랙은 ID 로만 가진다.
 *        마치면 추천 트랙이 있고, 건너뛰면 없다 (DB CHECK ck_diagnosis_result_track).
 *        마침·건너뜀은 createCompleted·createSkipped 로만 만들어 상태와 추천 트랙이 어긋나지 않게 한다 (#99).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diagnosisResultId;

    private Long memberId;

    private Long recommendedTrackId;    // 건너뛰면 NULL

    @Enumerated(EnumType.STRING)
    private DiagnosisStatus status;

    private DiagnosisResult(Long memberId, Long recommendedTrackId, DiagnosisStatus status) {
        this.memberId = memberId;
        this.recommendedTrackId = recommendedTrackId;
        this.status = status;
    }

    /* 진단을 마침: 추천 트랙이 꼭 있다 (API-DGN-02) */
    public static DiagnosisResult createCompleted(Long memberId, Long recommendedTrackId) {
        return new DiagnosisResult(memberId, recommendedTrackId, DiagnosisStatus.COMPLETED);
    }

    /* 진단을 건너뜀: 추천 트랙이 없다 (API-DGN-03) */
    public static DiagnosisResult createSkipped(Long memberId) {
        return new DiagnosisResult(memberId, null, DiagnosisStatus.SKIPPED);
    }

    public boolean isCompleted() {
        return status == DiagnosisStatus.COMPLETED;
    }
}
