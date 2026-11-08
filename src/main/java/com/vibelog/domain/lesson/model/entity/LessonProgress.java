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
 * [LessonProgress]
 * 역할 : 수업 진도 테이블(lesson_progress)과 매핑된다. 수강(enrollment) 하나가 수업 하나를 얼마나 진행했는지 담는다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 lesson_progress 그대로 둔다.
 *        열림·완료 기록은 "처음 한 번만" 기록한다 (계약 ③).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lessonProgressId;

    private Long enrollmentId;

    private Long lessonId;

    private LocalDateTime unlockedAt;

    private LocalDateTime completedAt;

    private LessonProgress(Long enrollmentId, Long lessonId, LocalDateTime unlockedAt) {
        this.enrollmentId = enrollmentId;
        this.lessonId = lessonId;
        this.unlockedAt = unlockedAt;
    }

    public static LessonProgress create(Long enrollmentId, Long lessonId, LocalDateTime unlockedAt) {
        return new LessonProgress(enrollmentId, lessonId, unlockedAt);
    }

    /* 잠긴 수업을 연다. 이미 열려 있으면 다시 기록하지 않는다 */
    public void unlock() {
        if (unlockedAt == null) {
            unlockedAt = LocalDateTime.now();
        }
    }

    /* 마지막 퀴즈의 모의고사 리포트를 확인하면 수업을 완료한다 (계약 ③). 처음 한 번만 기록한다 */
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
