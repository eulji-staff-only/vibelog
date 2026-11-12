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
 *        만들기(create)·수정(update)은 트랙 관리(운영자, API-ADM-03·04, #95)가 쓴다.
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

    private Track(String name, String target, String description, int sortOrder) {
        this.name = name;
        this.target = target;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public static Track create(String name, String target, String description, int sortOrder) {
        return new Track(name, target, description, sortOrder);
    }

    public void update(String name, String target, String description, int sortOrder) {
        this.name = name;
        this.target = target;
        this.description = description;
        this.sortOrder = sortOrder;
    }
}
