package com.vibelog.domain.portfolio.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 * [PortfolioAssignmentForm]
 * 역할 : 과제·스텝 저장 폼 (API-ADM-35).
 * 책임 : - steps 는 보낸 순서가 곧 스텝 순서(sort_order)다. 새 스텝은 guideStepId 를 비운다.
 *        - 폼 값을 목록으로 묶어 받으려고 setter 가 있는 클래스로 둔다 (Entity 가 아님).
 *        - 길이 제한은 sql/01-schema.sql 컬럼 크기 (topic VARCHAR(200)).
 */
@Getter
@Setter
@NoArgsConstructor
public class PortfolioAssignmentForm {

    @NotBlank(message = "과제 주제를 입력해 주세요.")
    @Size(max = 200, message = "과제 주제는 200자까지 입력할 수 있습니다.")
    private String topic;

    @NotBlank(message = "학습 목표를 입력해 주세요.")
    private String goal;

    @Valid
    private List<Step> steps = new ArrayList<>();

    /* [Step] 역할 : 스텝 한 줄의 입력 값. 책임 : 새 스텝은 guideStepId 를 비우고, 내용(content)은 비울 수 없다. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Step {
        private Long guideStepId;

        @NotBlank(message = "스텝 내용을 입력해 주세요.")
        private String content;

        public Step(Long guideStepId, String content) {
            this.guideStepId = guideStepId;
            this.content = content;
        }
    }

    /* 저장된 과제로 편집 폼을 채운다 */
    public static PortfolioAssignmentForm of(String topic, String goal, List<Step> steps) {
        PortfolioAssignmentForm form = new PortfolioAssignmentForm();
        form.topic = topic;
        form.goal = goal;
        form.steps = new ArrayList<>(steps);
        return form;
    }
}
