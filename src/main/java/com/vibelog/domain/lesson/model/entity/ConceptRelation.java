package com.vibelog.domain.lesson.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [ConceptRelation]
 * 역할 : 연관 개념 테이블(concept_relation)과 매핑된다. 개념 하나(source)에서 이어서 볼 다른 개념(related)을 순서대로 가리킨다 (FR-LSN-003).
 * 책임 : 컬럼은 sql/01-schema.sql 의 concept_relation 그대로 둔다. 운영자 입력 화면은 없고 샘플 데이터로 채운다.
 *        자기 자신을 가리키는 행은 DB 체크(ck_concept_relation_self)가 막는다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConceptRelation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conceptRelationId;

    private Long sourceActivityId;

    private Long relatedActivityId;

    private int sortOrder;

    private ConceptRelation(Long sourceActivityId, Long relatedActivityId, int sortOrder) {
        this.sourceActivityId = sourceActivityId;
        this.relatedActivityId = relatedActivityId;
        this.sortOrder = sortOrder;
    }

    public static ConceptRelation create(Long sourceActivityId, Long relatedActivityId, int sortOrder) {
        return new ConceptRelation(sourceActivityId, relatedActivityId, sortOrder);
    }
}
