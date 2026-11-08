package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.model.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * [AuthAdminMemberController]
 * 역할 : 백오피스 회원 목록·상세(A-08) 요청을 받는다 (API-ADM-37·38, #119). 코드는 auth 영역에 둔다 (이슈 #119).
 * 책임 : 운영자만 들어온다 (SecurityConfig 가 /admin/** 을 막는다). 없는 회원은 USER_NOT_FOUND → 오류 화면.
 */
@Controller
@RequiredArgsConstructor
public class AuthAdminMemberController {

    private final AdminMemberService adminMemberService;

    /* API-ADM-37 */
    @GetMapping("/admin/members")
    public String list(@RequestParam(required = false) String q, @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("members", adminMemberService.search(q, page));
        model.addAttribute("q", q == null ? "" : q.strip());
        return "auth/admin-members";
    }

    /* API-ADM-38 */
    @GetMapping("/admin/members/{memberId}")
    public String detail(@PathVariable Long memberId, Model model) {
        model.addAttribute("member", adminMemberService.getDetail(memberId));
        return "auth/admin-member";
    }
}
