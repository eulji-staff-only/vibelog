package com.vibelog.domain.course.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [DiagnosisQuestion]
 * 역할 : 진단 문항 테이블(diagnosis_question)과 매핑된다. 진단 설문의 질문 하나다 (FR-DGN-001).
 * 책임 : 컬럼은 sql/01-schema.sql 의 diagnosis_question 그대로 둔다. 설문에는 사용 중(is_active)인 문항만 보인다.
 *        문항 관리 메서드는 진단 설정(P2, #187)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diagnosisQuestionId;

    private String content;

    private int sortOrder;

    private boolean isActive;
}
