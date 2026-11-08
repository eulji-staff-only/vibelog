package com.vibelog.global.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.service.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [MainController]
 * 역할 : 메인(랜딩) 화면 `/` (U-01) 을 보여준다. 어느 도메인 영역에도 속하지 않는 화면이라 global 에 둔다 (#13).
 * 책임 : 화면 이름만 돌려준다. 회원이면 공통 헤더(1-5)에 쓸 닉네임만 MemberReader 로 읽어 모델에 담는다
 *        (#132, README "로그인 약속" — 로그인 회원은 Controller 에서만 꺼내고 memberId 만 넘긴다).
 *        그 외 추천 데이터 연결은 각 영역 기능이 준비된 뒤 추가한다 (지금은 화면 흐름 설계서 기준 샘플 문구로 채워진 화면).
 */
@Controller
@RequiredArgsConstructor
public class MainController {

    private final MemberReader memberReader;

    @GetMapping("/")
    public String main(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        if (loginMember != null) {
            model.addAttribute("loginNickname", memberReader.getNickname(loginMember.getMemberId()));
        }
        return "common/main";
    }
}
