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
 * 책임 : 컬럼은 sql/01-schema.sql 의 course_category 그대로 둔다. 이름은 UNIQUE 다 (순서는 UNIQUE 아님).
 *        만들기·이름 바꾸기·순서 바꾸기는 카테고리 관리(운영자, API-ADM-07·08·10, #95)가 쓴다.
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

    private CourseCategory(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public static CourseCategory create(String name, int sortOrder) {
        return new CourseCategory(name, sortOrder);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void moveTo(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
