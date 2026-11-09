package com.vibelog.domain.message.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* 답장(API-MSG-05)·공개 프로필 쪽지(API-MSG-04) form content. 1~1,000자 (PL-MSG-003) */
@Getter
@Setter
@NoArgsConstructor
public class MessageContentRequest {

    public static final String CONTENT_RULE = "쪽지 내용을 1~1,000자로 입력해 주세요";

    @NotBlank(message = CONTENT_RULE)
    @Size(max = 1000, message = CONTENT_RULE)
    private String content;
}
