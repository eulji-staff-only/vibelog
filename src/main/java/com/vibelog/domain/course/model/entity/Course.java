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
 *        - 새 코스는 비공개로 만든다 (FR-CRS-005). 공개·비공개는 공개 조건을 봐야 해서 따로 둔다 (API-ADM-16).
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

    /* 운영자 코스 만들기 (API-ADM-13). 항상 비공개로 시작한다 */
    public static Course create(Long trackId, Long courseCategoryId, String title, String summary, String description,
                                String target, String goal, int planDays) {
        Course course = new Course();
        course.change(trackId, courseCategoryId, title, summary, description, target, goal, planDays);
        course.isPublic = false;
        return course;
    }

    /* 운영자 코스 수정 (API-ADM-15). 공개 상태는 바꾸지 않는다 */
    public void change(Long trackId, Long courseCategoryId, String title, String summary, String description,
                       String target, String goal, int planDays) {
        this.trackId = trackId;
        this.courseCategoryId = courseCategoryId;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.target = target;
        this.goal = goal;
        this.planDays = planDays;
    }
}
