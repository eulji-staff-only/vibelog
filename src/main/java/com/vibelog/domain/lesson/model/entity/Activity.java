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
 *        추가·수정·사용 중지·순서 변경(API-ADM-21~24, #101)은 이 안의 도메인 메서드로 한다.
 *        삭제 메서드는 두지 않는다 — 학습자 진도 보존을 위해 사용 중지(deactivate)만 한다.
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

    /*
     * 활동 수정 (API-ADM-22). 사용 중지 여부·순서는 따로 바꾼다.
     * activityType은 바꾸지 못한다 — 모의고사 열림 계약 ②(마지막 퀴즈 진도 완료)와 exam_question.concept_activity_id가
     * 활동 종류를 보고 판단·참조하기 때문에, 만든 뒤 종류가 바뀌면 그 판단·참조가 깨진다. 종류를 바꾸려면 사용 중지
     * 후 새로 추가한다 (Choyeonik 리뷰, PR #217).
     */
    public void edit(String title, String body, BodyFormat bodyFormat,
                      String toolNoteUrl, Integer estimatedMinutes) {
        this.title = title;
        this.body = body;
        this.bodyFormat = bodyFormat;
        this.toolNoteUrl = toolNoteUrl;
        this.estimatedMinutes = estimatedMinutes;
    }

    /* 활동 사용 중지 (API-ADM-23). 삭제하지 않아 학습자 진도(activity_progress)는 그대로 남는다 */
    public void deactivate() {
        this.isActive = false;
    }

    /* 활동 순서 변경 (API-ADM-24) */
    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
