package com.vibelog.domain.message.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* 쪽지함 새 쪽지 (API-MSG-03 form receiverMemberId, content). 받는 사람은 닉네임 검색(API-MSG-02)으로 고른다 */
@Getter
@Setter
@NoArgsConstructor
public class MessageWriteRequest {

    @NotNull(message = "받는 사람을 닉네임으로 찾아 골라 주세요")
    private Long receiverMemberId;

    @NotBlank(message = MessageContentRequest.CONTENT_RULE)
    @Size(max = 1000, message = MessageContentRequest.CONTENT_RULE)
    private String content;
}
