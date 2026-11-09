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
 * [CourseCategory]
 * 역할 : 코스 카테고리 테이블(course_category)과 매핑된다. 코스를 주제별로 나눈다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 course_category 그대로 둔다. 이름은 UNIQUE 다.
 *        만들기·수정 메서드는 카테고리 관리(운영자, #95)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseCategory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseCategoryId;

    private String name;

    private int sortOrder;
}
