package com.vibelog.domain.exam.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [ExamChoice]
 * 역할 : 모의고사 문항의 보기 (exam_choice).
 * 책임 : 정답 여부(is_correct)를 가진다. 같은 영역의 문항은 연관관계로 참조한다.
 */
@Getter
@Entity
@Table(name = "exam_choice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamChoice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long examChoiceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_question_id", nullable = false)
    private ExamQuestion examQuestion;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int sortOrder;
}
