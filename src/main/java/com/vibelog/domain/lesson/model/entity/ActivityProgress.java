package com.vibelog.domain.lesson.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [ActivityProgress]
 * 역할 : 활동 진도 테이블(activity_progress)과 매핑된다. 수강(enrollment) 하나가 활동 하나를 얼마나 진행했는지 담는다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 activity_progress 그대로 둔다.
 *        열림·완료 기록은 "처음 한 번만" 기록한다 (계약 ①).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityProgressId;

    private Long enrollmentId;

    private Long activityId;

    private LocalDateTime unlockedAt;

    private LocalDateTime completedAt;

    private ActivityProgress(Long enrollmentId, Long activityId, LocalDateTime unlockedAt) {
        this.enrollmentId = enrollmentId;
        this.activityId = activityId;
        this.unlockedAt = unlockedAt;
    }

    public static ActivityProgress create(Long enrollmentId, Long activityId, LocalDateTime unlockedAt) {
        return new ActivityProgress(enrollmentId, activityId, unlockedAt);
    }

    /* 잠긴 활동을 연다. 이미 열려 있으면 다시 기록하지 않는다 */
    public void unlock() {
        if (unlockedAt == null) {
            unlockedAt = LocalDateTime.now();
        }
    }

    /* 개념을 읽거나 퀴즈를 통과하면 활동을 완료한다 (계약 ①, FR-LSN-002). 처음 한 번만 기록한다 */
    public void complete() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    public boolean isUnlocked() {
        return unlockedAt != null;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }
}
