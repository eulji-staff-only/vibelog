package com.vibelog.domain.community.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* 리뷰 코멘트 1개 (API-REV-03·04 form comments[i].reviewCriterionId, comments[i].content). 기준이 없으면 reviewCriterionId 없이 */
@Getter
@Setter
@NoArgsConstructor
public class ReviewCommentInput {

    public static final String CONTENT_RULE = "코멘트를 1~1,000자로 입력해 주세요";

    private Long reviewCriterionId;

    @NotBlank(message = CONTENT_RULE)
    @Size(max = 1000, message = CONTENT_RULE)
    private String content;

    public ReviewCommentInput(Long reviewCriterionId, String content) {
        this.reviewCriterionId = reviewCriterionId;
        this.content = content;
    }
}
