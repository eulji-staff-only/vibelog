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
 * [DiagnosisOption]
 * 역할 : 진단 보기 테이블(diagnosis_option)과 매핑된다. 문항의 보기 하나이고, 고르면 연결된 트랙에 점수가 쌓인다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 diagnosis_option 그대로 둔다. 문항·트랙은 같은 영역이지만 ID 로만 가진다.
 *        트랙 점수 합계로 추천 트랙을 정하는 규칙은 진단 기능(#99, PL-DGN-001·002)에서 만든다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diagnosisOptionId;

    private Long diagnosisQuestionId;

    private Long trackId;

    private String content;

    private int score;

    private int sortOrder;
}
