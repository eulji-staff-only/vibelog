package com.vibelog.domain.exam.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * [ExamAttempt]
 * 역할 : 회원의 수업별 모의고사 응시 1회 (exam_attempt).
 * 책임 : 회원·수업은 다른 영역이라 ID 로만 가진다.
 *        제출 전에는 score·correct_rate·submitted_at 이 비어 있다 (DB CHECK ck_exam_attempt_submitted).
 *        회차는 1부터 늘어난다 (PL-EXM-003). 리포트를 처음 열면 연 일시를 남긴다 (PL-EXM-007, #111).
 */
@Getter
@Entity
@Table(name = "exam_attempt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamAttempt extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long examAttemptId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private int attemptNo;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private Integer score;

    @Column(precision = 5, scale = 2)
    private BigDecimal correctRate;

    private LocalDateTime submittedAt;

    private LocalDateTime reportViewedAt;

    /* 새 회차를 시작한다. 시작 일시는 지금 */
    public static ExamAttempt create(Long memberId, Long lessonId, int attemptNo) {
        ExamAttempt attempt = new ExamAttempt();
        attempt.memberId = memberId;
        attempt.lessonId = lessonId;
        attempt.attemptNo = attemptNo;
        attempt.startedAt = LocalDateTime.now();
        return attempt;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    public boolean isReportViewed() {
        return reportViewedAt != null;
    }

    /* 리포트를 처음 연 일시를 남긴다. 다시 열면 처음 일시를 그대로 둔다 (PL-EXM-007) */
    public void markReportViewed() {
        if (reportViewedAt == null) {
            reportViewedAt = LocalDateTime.now();
        }
    }

    /* 채점 결과를 남기고 제출한다. score = 맞힌 문항 수, correctRate = 정답률(%) */
    public void submit(int score, BigDecimal correctRate) {
        this.score = score;
        this.correctRate = correctRate;
        this.submittedAt = LocalDateTime.now();
    }
}
