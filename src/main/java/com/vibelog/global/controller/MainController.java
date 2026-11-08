package com.vibelog.global.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.message.model.service.MessageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [MainController]
 * 역할 : 메인(랜딩) 화면 `/` (U-01, API-USR-01) 을 보여준다. 어느 도메인 영역에도 속하지 않는 화면이라 global 에 둔다 (#13).
 * 책임 : - 회원이면 공통 헤더(1-5)에 쓸 닉네임을 MemberReader 로 읽어 모델에 담는다 (#132, README "로그인 약속").
 *        - P2 (#203): 최근 공지(MessageReader)와 이번 주 추천 작품 상위 3개(CommunityReader)를 각 영역 Reader 로 받는다.
 *        - 이달의 콘테스트는 contest 영역(#200~#202)이 생기면 ContestReader 로 붙인다. 그 전에는 빈 값이다 (명세 API-USR-01).
 */
@Controller
@RequiredArgsConstructor
public class MainController {

    /* 명세는 "최근 공지 N개"라 정해지지 않아 3개로 둔다 (#203 PR 에서 확인) */
    static final int RECENT_NOTICES = 3;
    static final int WEEKLY_TOP = 3;

    private final MemberReader memberReader;
    private final MessageReader messageReader;
    private final CommunityReader communityReader;

    @GetMapping("/")
    public String main(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        if (loginMember != null) {
            model.addAttribute("loginNickname", memberReader.getNickname(loginMember.getMemberId()));
        }
        model.addAttribute("recentNotices", messageReader.findRecentNotices(RECENT_NOTICES));
        model.addAttribute("weeklyTop", communityReader.findWeeklyTop(WEEKLY_TOP));
        return "common/main";
    }
}
