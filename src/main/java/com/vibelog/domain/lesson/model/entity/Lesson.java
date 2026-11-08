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
 * [Lesson]
 * 역할 : 수업 테이블(lesson)과 매핑된다. 코스 하나는 여러 수업을 순서대로 가진다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 lesson 그대로 둔다.
 *        공개·비공개 전환, 수정 메서드는 수업 관리 기능 이슈(#100, #118)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lessonId;

    private Long courseId;

    private String title;

    private String intro;

    private String goal;

    private String contents;

    private int estimatedMinutes;

    private int sortOrder;

    private boolean isPublic;

    private Lesson(Long courseId, String title, String intro, String goal, String contents,
                   int estimatedMinutes, int sortOrder) {
        this.courseId = courseId;
        this.title = title;
        this.intro = intro;
        this.goal = goal;
        this.contents = contents;
        this.estimatedMinutes = estimatedMinutes;
        this.sortOrder = sortOrder;
        this.isPublic = false;
    }

    public static Lesson create(Long courseId, String title, String intro, String goal, String contents,
                                 int estimatedMinutes, int sortOrder) {
        return new Lesson(courseId, title, intro, goal, contents, estimatedMinutes, sortOrder);
    }

    /* 수업 정보 수정 (API-ADM-20). 공개 여부·순서는 따로 바꾼다 */
    public void edit(String title, String intro, String goal, String contents, int estimatedMinutes) {
        this.title = title;
        this.intro = intro;
        this.goal = goal;
        this.contents = contents;
        this.estimatedMinutes = estimatedMinutes;
    }

    /* 수업 순서 변경 (API-ADM-18) */
    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
