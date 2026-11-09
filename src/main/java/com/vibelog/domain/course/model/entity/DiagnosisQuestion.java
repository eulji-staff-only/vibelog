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
 *        문항은 지우지 않고 사용·중지만 한다 (진단 설정 API-ADM-42~44, #187).
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

    /* 문항 추가 (API-ADM-42). 사용 중으로 시작한다 */
    public static DiagnosisQuestion create(String content, int sortOrder) {
        DiagnosisQuestion question = new DiagnosisQuestion();
        question.content = content;
        question.sortOrder = sortOrder;
        question.isActive = true;
        return question;
    }

    /* 문항 수정 (API-ADM-43). 사용 여부는 바꾸지 않는다 */
    public void edit(String content, int sortOrder) {
        this.content = content;
        this.sortOrder = sortOrder;
    }

    /* 사용·중지 (API-ADM-44). 중지한 문항은 설문에 나오지 않는다 */
    public void changeActive(boolean active) {
        this.isActive = active;
    }
}
