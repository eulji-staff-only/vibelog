package com.vibelog.domain.quiz.model.entity;

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
 * [QuizQuestion]
 * 역할 : 퀴즈 문항 테이블(quiz_question)과 매핑된다. 활동(퀴즈) 하나는 문항을 여러 개 가진다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 quiz_question 그대로 둔다. 활동·개념 태그는 lesson 영역이라 ID 로만 가진다.
 *        수업 공개 판단(#118)이 activityId·isActive 만 먼저 매핑했고, 퀴즈 풀이(#108)에서 나머지 컬럼을 읽으려고 채웠다.
 *        등록(create)·수정(edit)·사용 중지(changeActive)는 퀴즈 문항 등록(운영자, #102)이 쓴다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizQuestionId;

    private Long activityId;

    private Long conceptTagId;

    @Enumerated(EnumType.STRING)
    private QuizDisplayType displayType;

    private String content;

    private String codeSnippet;     // 코드 빈칸 채우기·결과 유추하기만 있다 (DB CHECK ck_quiz_question_code_snippet)

    private String explanation;

    private int sortOrder;

    private boolean isActive;

    /* 운영자 문항 추가 (API-ADM-26). 사용 중으로 시작한다. 코드가 필요 없는 유형이면 codeSnippet 은 null */
    public static QuizQuestion create(Long activityId, Long conceptTagId, QuizDisplayType displayType, String content,
                                      String codeSnippet, String explanation, int sortOrder) {
        QuizQuestion question = new QuizQuestion();
        question.activityId = activityId;
        question.sortOrder = sortOrder;
        question.isActive = true;
        question.edit(conceptTagId, displayType, content, codeSnippet, explanation);
        return question;
    }

    /* 운영자 문항 수정 (API-ADM-27). 풀이 기록이 없는 문항만 이렇게 고친다 (있으면 새 문항으로, DB 설계서 5-4) */
    public void edit(Long conceptTagId, QuizDisplayType displayType, String content, String codeSnippet, String explanation) {
        this.conceptTagId = conceptTagId;
        this.displayType = displayType;
        this.content = content;
        this.codeSnippet = codeSnippet;
        this.explanation = explanation;
    }

    /* 사용·중지 (API-ADM-28). 중지한 문항은 다음 풀이부터 나오지 않는다 */
    public void changeActive(boolean active) {
        this.isActive = active;
    }
}
