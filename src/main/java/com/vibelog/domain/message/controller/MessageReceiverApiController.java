package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.model.dto.ReceiverItem;
import com.vibelog.domain.message.model.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * [MessageReceiverApiController]
 * 역할 : 쪽지함 새 쪽지의 받는 사람 찾기 (API-MSG-02). 닉네임 앞부분이 같은 회원 최대 10명 (명세 결정 3).
 * 책임 : 회원만 (SecurityConfig). 나와 탈퇴 회원은 뺀다.
 */
@RestController
@RequiredArgsConstructor
public class MessageReceiverApiController {

    private final MessageService messageService;

    @GetMapping("/api/users")
    public List<ReceiverItem> search(@RequestParam(required = false) String nickname, @AuthenticationPrincipal LoginMember loginMember) {
        return messageService.findReceivers(loginMember.getMemberId(), nickname);
    }
}
