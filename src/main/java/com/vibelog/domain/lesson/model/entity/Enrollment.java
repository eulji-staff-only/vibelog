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
 * [Enrollment]
 * 역할 : 수강 테이블(enrollment)과 매핑된다. 회원 한 명이 코스 하나를 수강한 기록이다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 enrollment 그대로 둔다.
 *        포트폴리오 열림·코스 완료 기록은 "처음 한 번만" 기록한다 (계약 ③·⑦).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long enrollmentId;

    private Long memberId;

    private Long courseId;

    private LocalDateTime startedAt;

    private LocalDateTime portfolioUnlockedAt;

    private LocalDateTime completedAt;

    private LocalDateTime droppedAt;

    private Enrollment(Long memberId, Long courseId, LocalDateTime startedAt) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.startedAt = startedAt;
    }

    public static Enrollment create(Long memberId, Long courseId, LocalDateTime startedAt) {
        return new Enrollment(memberId, courseId, startedAt);
    }

    /* 마지막 수업 리포트를 확인하면 포트폴리오가 열린다 (계약 ③). 이미 열려 있으면 다시 기록하지 않는다 */
    public void unlockPortfolio() {
        if (portfolioUnlockedAt == null) {
            portfolioUnlockedAt = LocalDateTime.now();
        }
    }

    /* 모든 공개 수업을 완료하고 포트폴리오를 제출하면 코스를 완료한다 (계약 ⑦). 처음 한 번만 기록한다 */
    public void complete() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    /* 포트폴리오를 삭제하면 코스 완료 기록을 되돌린다 (계약 ⑦, PL-ENR-003) */
    public void cancelCompletion() {
        completedAt = null;
    }

    /* 수강을 포기하면 내 학습 목록에서 내린다. 진행 기록은 지우지 않는다 (API-ENR-04, PL-ENR-004). 처음 한 번만 기록한다 */
    public void drop() {
        if (droppedAt == null) {
            droppedAt = LocalDateTime.now();
        }
    }

    /* 포기한 코스를 다시 시작하면 포기 기록만 비우고 이전 진행 상태에서 이어서 한다 (API-ENR-01 P2, PL-ENR-004) */
    public void restart() {
        droppedAt = null;
    }

    public boolean isPortfolioUnlocked() {
        return portfolioUnlockedAt != null;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public boolean isDropped() {
        return droppedAt != null;
    }

    public boolean isActive() {
        return completedAt == null && droppedAt == null;
    }
}
