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
 *        응시 시작·제출·리포트 확인 메서드는 #110·#111 에서 추가한다.
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
}
