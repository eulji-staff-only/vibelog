package com.vibelog.domain.lesson.model.dto;

import com.vibelog.domain.lesson.model.entity.Lesson;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/*
 * [LessonFormRequest]
 * 역할 : 수업 만들기·수정 폼 값 (API-ADM-17 form title, intro, goal, contents, estimatedMinutes / API-ADM-20 같음).
 * 책임 : - 제목 길이(100자)는 sql/01-schema.sql lesson.title 그대로다. 그 밖의 숫자 제한은 요구사항·정책 문서에
 *          없어 지어내지 않고 필수 입력만 검사한다.
 */
@Getter
@Setter
public class LessonFormRequest {

    @NotBlank(message = "수업 제목을 입력해 주세요")
    @Size(max = 100, message = "수업 제목은 100자까지 쓸 수 있습니다")
    private String title;

    @NotBlank(message = "수업 소개를 입력해 주세요")
    private String intro;

    @NotBlank(message = "학습 목표를 입력해 주세요")
    private String goal;

    @NotBlank(message = "본문을 입력해 주세요")
    private String contents;

    @NotNull(message = "예상 소요 시간(분)을 입력해 주세요")
    @Min(value = 1, message = "예상 소요 시간은 1분 이상으로 입력해 주세요")
    private Integer estimatedMinutes;

    /* 수정 화면(A-06)에 기존 내용을 채운다 */
    public static LessonFormRequest from(Lesson lesson) {
        LessonFormRequest request = new LessonFormRequest();
        request.title = lesson.getTitle();
        request.intro = lesson.getIntro();
        request.goal = lesson.getGoal();
        request.contents = lesson.getContents();
        request.estimatedMinutes = lesson.getEstimatedMinutes();
        return request;
    }
}
