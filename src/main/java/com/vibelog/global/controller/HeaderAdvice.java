package com.vibelog.global.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.service.MemberReader;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/*
 * [HeaderAdvice]
 * 역할 : 모든 화면의 공통 헤더(common/layout.html, 1-5)에 로그인 회원 닉네임 "loginNickname" 을 넣는다 (#362).
 * 책임 : 비회원이면 null (헤더는 비회원 메뉴를 보여 준다). 닉네임은 마이페이지에서 바뀔 수 있어 세션에 두지 않고 매번 읽는다.
 *        MemberReader 는 ObjectProvider 로 받는다. @WebMvcTest 화면 테스트에 빈이 없으면 null 로 넘어간다 (MessageHeaderAdvice 와 같음).
 *        JSON API(/api/**)는 헤더를 그리지 않으므로 읽지 않는다.
 *        작성 중 내용 보관(#366, request.js)의 주인 "draftOwner" 도 넣는다. 같은 컴퓨터의 다른 회원에게 남의 임시 저장이 보이지 않게
 *        회원 ID 로 키를 나눈다. 조회 없이 로그인 정보에서 꺼낸다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class HeaderAdvice {

    private final ObjectProvider<MemberReader> memberReader;

    @ModelAttribute("loginNickname")
    public String loginNickname(@AuthenticationPrincipal LoginMember loginMember, HttpServletRequest request) {
        MemberReader reader = memberReader.getIfAvailable();
        return loginMember == null || reader == null || request.getServletPath().startsWith("/api/") ? null
                : reader.getNickname(loginMember.getMemberId());
    }

    @ModelAttribute("draftOwner")
    public Long draftOwner(@AuthenticationPrincipal LoginMember loginMember) {
        return loginMember == null ? null : loginMember.getMemberId();
    }
}
