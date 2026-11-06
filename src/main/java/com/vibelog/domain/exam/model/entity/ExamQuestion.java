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

/*
 * [ExamQuestion]
 * 역할 : 모의고사 문항 (exam_question).
 * 책임 : 수업(lesson)·연관 개념(activity)은 다른 영역이라 ID 로만 가진다.
 *        문항 만들기·수정은 운영자 문항 관리(#103)에서 추가한다.
 */
@Getter
@Entity
@Table(name = "exam_question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long examQuestionId;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private Long conceptActivityId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
