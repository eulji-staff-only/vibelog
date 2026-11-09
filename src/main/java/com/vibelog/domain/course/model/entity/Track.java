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
 * [Track]
 * 역할 : 트랙 테이블(track)과 매핑된다. 코스를 학습 목표별로 묶는 큰 갈래다 (FR-CRS-004).
 * 책임 : 컬럼은 sql/01-schema.sql 의 track 그대로 둔다. 이름·순서(sort_order)는 각각 UNIQUE 다.
 *        만들기·수정 메서드는 트랙 관리(운영자, #95)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trackId;

    private String name;

    private String target;

    private String description;

    private int sortOrder;
}
