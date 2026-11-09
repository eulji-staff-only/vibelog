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
 * 역할 : 트랙 테이블(track)과 매핑된다. 코스는 트랙 하나에 속한다 (FR-CRS-007).
 * 책임 : 컬럼은 sql/01-schema.sql 의 track 그대로 둔다.
 *        운영자 대시보드(admin #120)가 트랙 이름을 읽으려고 먼저 만들었다. 트랙 관리(#82, A-02)가
 *        만들기(create)·수정 메서드는 쓰는 곳이 생길 때 추가한다.
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
