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
 * [ExamAnswer]
 * 역할 : 응시 1회 안에서 문항별로 고른 답 (exam_answer). 응시·문항 한 쌍에 한 행 (uk_exam_answer_attempt_question).
 * 책임 : 고른 보기가 없으면(선택 취소·미선택) exam_choice_id 가 비어 있다. 정답 여부는 채점 때 채운다.
 *        답은 응시 중 몇 번이든 바꿀 수 있고(PL-EXM-006), 제출할 때 채점한다.
 */
@Getter
@Entity
@Table(name = "exam_answer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long examAnswerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_attempt_id", nullable = false)
    private ExamAttempt examAttempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_question_id", nullable = false)
    private ExamQuestion examQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_choice_id")
    private ExamChoice examChoice;

    @Column(name = "is_correct")
    private Boolean correct;

    /* 고른 보기는 비어 있을 수 있다 (선택 취소·미선택) */
    public static ExamAnswer create(ExamAttempt examAttempt, ExamQuestion examQuestion, ExamChoice examChoice) {
        ExamAnswer answer = new ExamAnswer();
        answer.examAttempt = examAttempt;
        answer.examQuestion = examQuestion;
        answer.examChoice = examChoice;
        return answer;
    }

    public void choose(ExamChoice examChoice) {
        this.examChoice = examChoice;
    }

    /* 고른 보기가 정답이면 맞음. 고르지 않았으면 틀림 */
    public boolean grade() {
        this.correct = examChoice != null && examChoice.isCorrect();
        return correct;
    }
}
