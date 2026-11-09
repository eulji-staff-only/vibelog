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
 *        운영자가 만들고(create)·고치고(edit)·사용 중지(changeActive)한다 (API-ADM-29~31, #103).
 *        응시 기록이 문항을 가리키므로 지우지 않고 사용 중지만 한다.
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

    public static ExamQuestion create(Long lessonId, Long conceptActivityId, String content, String explanation,
                                      int sortOrder) {
        ExamQuestion question = new ExamQuestion();
        question.lessonId = lessonId;
        question.conceptActivityId = conceptActivityId;
        question.content = content;
        question.explanation = explanation;
        question.sortOrder = sortOrder;
        question.active = true;
        return question;
    }

    /* API-ADM-30. 응시 기록이 없는 문항만 고친다 (기록이 있으면 사용 중지 후 새 문항, DB 설계서 5-4). 순서·사용 여부는 그대로 */
    public void edit(Long conceptActivityId, String content, String explanation) {
        this.conceptActivityId = conceptActivityId;
        this.content = content;
        this.explanation = explanation;
    }

    /* API-ADM-31. 사용 중지한 문항은 다음 응시부터 나오지 않는다 */
    public void changeActive(boolean active) {
        this.active = active;
    }
}
