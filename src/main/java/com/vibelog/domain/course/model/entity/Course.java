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
 * [Course]
 * 역할 : 코스 테이블(course)과 매핑된다. 트랙·카테고리에 속하고, 수업(lesson)을 순서대로 가진다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 course 그대로 둔다. 트랙·카테고리는 ID 로만 가진다.
 *        운영자 회원 상세(#119)가 코스 이름을 읽으려고 먼저 만들었다. course 기반(#83)에서 track·course_category 등
 *        나머지 Entity 와 만들기(create)·수정 메서드는 쓰는 곳이 생길 때 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    private Long trackId;

    private Long courseCategoryId;

    private String title;

    private String summary;

    private String description;

    private String target;

    private String goal;

    private int planDays;

    private boolean isPublic;
}
