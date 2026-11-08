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
 *        로그인 도착지(#12, 계약 ⑫)가 기록이 있는지만 보려고 먼저 만들었다. course 기반(#83)·진단(#99)에서
 *        나머지 진단 Entity 와 만들기(create) 메서드를 이어서 추가한다.
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
}
