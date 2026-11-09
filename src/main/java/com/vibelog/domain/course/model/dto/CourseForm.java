package com.vibelog.domain.course.model.dto;

import com.vibelog.domain.course.model.entity.Course;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 코스 만들기·수정 폼 (API-ADM-13·15, FR-CRS-005). 칸 길이는 sql/01-schema.sql 의 course 컬럼 그대로.
 * 폼 바인딩(th:field)에 쓰여 Setter 가 있다 (Entity 가 아니라 입력 DTO).
 */
@Getter
@Setter
@NoArgsConstructor
public class CourseForm {

    @NotNull(message = "트랙을 골라 주세요.")
    private Long trackId;

    @NotNull(message = "카테고리를 골라 주세요.")
    private Long courseCategoryId;

    @NotBlank(message = "코스 이름을 입력해 주세요.")
    @Size(max = 100, message = "코스 이름은 100자까지 쓸 수 있습니다.")
    private String title;

    @NotBlank(message = "한 줄 소개를 입력해 주세요.")
    @Size(max = 200, message = "한 줄 소개는 200자까지 쓸 수 있습니다.")
    private String summary;

    @NotBlank(message = "소개를 입력해 주세요.")
    private String description;

    @NotBlank(message = "대상 학습자를 입력해 주세요.")
    @Size(max = 200, message = "대상 학습자는 200자까지 쓸 수 있습니다.")
    private String target;

    @NotBlank(message = "목표를 입력해 주세요.")
    private String goal;

    @NotNull(message = "플랜 일수를 입력해 주세요.")
    @Min(value = 1, message = "플랜 일수는 1 이상이어야 합니다.")
    private Integer planDays;

    /* 상세 화면(A-05)에 기존 값을 채운다 */
    public static CourseForm from(Course course) {
        return of(course.getTrackId(), course.getCourseCategoryId(), course.getTitle(), course.getSummary(),
                course.getDescription(), course.getTarget(), course.getGoal(), course.getPlanDays());
    }

    public static CourseForm of(Long trackId, Long courseCategoryId, String title, String summary, String description,
                                String target, String goal, int planDays) {
        CourseForm form = new CourseForm();
        form.trackId = trackId;
        form.courseCategoryId = courseCategoryId;
        form.title = title;
        form.summary = summary;
        form.description = description;
        form.target = target;
        form.goal = goal;
        form.planDays = planDays;
        return form;
    }
}
