package com.vibelog.domain.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* 카테고리 만들기·수정 폼 (API-ADM-07·08). 이름 길이는 course_category.name VARCHAR(30) */
@Getter
@Setter
@NoArgsConstructor
public class CategoryForm {

    @NotBlank(message = "카테고리 이름을 입력해 주세요.")
    @Size(max = 30, message = "카테고리 이름은 30자까지 쓸 수 있습니다.")
    private String name;

    public static CategoryForm of(String name) {
        CategoryForm form = new CategoryForm();
        form.name = name;
        return form;
    }
}
