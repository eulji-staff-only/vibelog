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
 * [ConceptTag]
 * 역할 : 개념 태그 테이블(concept_tag)과 매핑된다. 개념 활동·퀴즈 문항·모의고사 문항이 이 태그로 연결된다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 concept_tag 그대로 둔다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConceptTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conceptTagId;

    private String name;

    private ConceptTag(String name) {
        this.name = name;
    }

    public static ConceptTag create(String name) {
        return new ConceptTag(name);
    }
}
