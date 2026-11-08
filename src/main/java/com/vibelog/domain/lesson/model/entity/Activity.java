package com.vibelog.domain.lesson.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [Activity]
 * 역할 : 활동 테이블(activity)과 매핑된다. 수업 하나는 개념·퀴즈 활동을 순서대로 가진다 (PL-LSN-001).
 * 책임 : 컬럼은 sql/01-schema.sql 의 activity 그대로 둔다.
 *        추가·수정·사용 중지·순서 변경 메서드는 활동 관리 기능 이슈(#101)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityId;

    private Long lessonId;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    private String title;

    private String body;

    @Enumerated(EnumType.STRING)
    private BodyFormat bodyFormat;

    private String toolNoteUrl;

    private Integer estimatedMinutes;

    private Integer passRate;

    private int sortOrder;

    private boolean isActive;

    private Activity(Long lessonId, ActivityType activityType, String title, String body, BodyFormat bodyFormat,
                      String toolNoteUrl, Integer estimatedMinutes, Integer passRate, int sortOrder) {
        this.lessonId = lessonId;
        this.activityType = activityType;
        this.title = title;
        this.body = body;
        this.bodyFormat = bodyFormat;
        this.toolNoteUrl = toolNoteUrl;
        this.estimatedMinutes = estimatedMinutes;
        this.passRate = passRate;
        this.sortOrder = sortOrder;
        this.isActive = true;
    }

    public static Activity create(Long lessonId, ActivityType activityType, String title, String body,
                                   BodyFormat bodyFormat, String toolNoteUrl, Integer estimatedMinutes,
                                   Integer passRate, int sortOrder) {
        return new Activity(lessonId, activityType, title, body, bodyFormat, toolNoteUrl, estimatedMinutes,
                passRate, sortOrder);
    }

    public boolean isQuiz() {
        return activityType == ActivityType.QUIZ;
    }

    public boolean isConcept() {
        return activityType == ActivityType.CONCEPT;
    }
}
