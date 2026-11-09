package com.vibelog.domain.lesson.model.dto;

import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.entity.BodyFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/*
 * [ActivityFormRequest]
 * 역할 : 활동 추가·수정 폼 값 (API-ADM-21 form activityType, title, body, bodyFormat, toolNoteUrl,
 *        estimatedMinutes, tagNames / API-ADM-22 같음).
 * 책임 : - title 길이(100자)·toolNoteUrl 길이(500자)는 sql/01-schema.sql activity 그대로다.
 *        - bodyFormat 은 받기만 하고, 지금 편집기가 textarea 하나뿐이라 실제 저장 값은 Service가 본문 유무로
 *          정한다 (본문 있으면 MARKDOWN, 없으면 null). 리치 텍스트 전환은 이 이슈 범위 밖이다 (사용자 결정).
 */
@Getter
@Setter
public class ActivityFormRequest {

    @NotNull(message = "활동 종류를 선택해 주세요")
    private ActivityType activityType;

    @NotBlank(message = "활동 제목을 입력해 주세요")
    @Size(max = 100, message = "활동 제목은 100자까지 쓸 수 있습니다")
    private String title;

    private String body;

    private BodyFormat bodyFormat;

    @Size(max = 500, message = "도구 노트 링크는 500자까지 쓸 수 있습니다")
    private String toolNoteUrl;

    @Min(value = 1, message = "예상 소요 시간은 1분 이상으로 입력해 주세요")
    private Integer estimatedMinutes;

    private List<String> tagNames;
}
