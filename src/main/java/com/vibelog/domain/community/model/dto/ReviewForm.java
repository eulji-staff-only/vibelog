package com.vibelog.domain.community.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 * 리뷰 쓰기·수정 폼 (API-REV-03·04). 코멘트마다 1~1,000자.
 * 보낸 기준 묶음이 그 과제의 활성 기준 묶음과 정확히 같은지는 DB 를 봐야 해서 PeerReviewService 가 본다 (INVALID_INPUT).
 */
@Getter
@Setter
@NoArgsConstructor
public class ReviewForm {

    @Valid
    @NotEmpty(message = ReviewCommentInput.CONTENT_RULE)
    private List<ReviewCommentInput> comments = new ArrayList<>();
}
