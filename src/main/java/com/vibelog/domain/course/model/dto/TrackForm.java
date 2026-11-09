package com.vibelog.domain.course.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 트랙 만들기·수정 폼 (API-ADM-03·04). 칸 길이는 sql/01-schema.sql 의 track 컬럼 그대로.
 * 폼 바인딩(th:field)에 쓰여 Setter 가 있다 (Entity 가 아니라 입력 DTO).
 */
@Getter
@Setter
@NoArgsConstructor
public class TrackForm {

    @NotBlank(message = "트랙 이름을 입력해 주세요.")
    @Size(max = 30, message = "트랙 이름은 30자까지 쓸 수 있습니다.")
    private String name;

    @NotBlank(message = "대상 학습자를 입력해 주세요.")
    @Size(max = 200, message = "대상 학습자는 200자까지 쓸 수 있습니다.")
    private String target;

    @NotBlank(message = "설명을 입력해 주세요.")
    private String description;

    @NotNull(message = "순서를 입력해 주세요.")
    @Min(value = 1, message = "순서는 1 이상이어야 합니다.")
    private Integer sortOrder;

    public static TrackForm of(String name, String target, String description, int sortOrder) {
        TrackForm form = new TrackForm();
        form.name = name;
        form.target = target;
        form.description = description;
        form.sortOrder = sortOrder;
        return form;
    }
}
