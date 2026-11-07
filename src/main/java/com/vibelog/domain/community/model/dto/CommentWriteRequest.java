package com.vibelog.domain.community.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/*
 * 댓글·답글 쓰기와 댓글 수정 폼 (API-CMT-08·09). 500자까지 (PL-CMT-006).
 * parentCommentId 는 답글일 때만 온다. 수정할 때는 쓰지 않는다.
 */
@Getter
@Setter
public class CommentWriteRequest {

    @NotBlank(message = "댓글을 입력해 주세요")
    @Size(max = 500, message = "댓글은 500자까지 쓸 수 있습니다")
    private String content;

    private Long parentCommentId;
}
