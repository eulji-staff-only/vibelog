package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.entity.SuspensionLevel;
import com.vibelog.domain.auth.model.service.AdminMemberService;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("levels", SuspensionLevel.values());
        return "auth/admin-member";
    }

    /* API-USR-19. A-08 사유·기간 모달에서 온다. 실패는 같은 화면 + toast (방식 ③) */
    @PostMapping("/admin/members/{memberId}/suspend")
    public String suspend(@PathVariable Long memberId, @RequestParam(required = false) SuspensionLevel level,
                          @RequestParam(required = false) String reason, @AuthenticationPrincipal LoginMember loginMember,
                          RedirectAttributes redirectAttributes) {
        if (level == null || reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("toast", "제재 단계와 사유를 입력해 주세요");
            return detailUrl(memberId);
        }
        return run(memberId, redirectAttributes, "제재를 저장했습니다",
                () -> adminMemberService.suspend(loginMember.getMemberId(), memberId, level, reason.strip()));
    }

    /* API-USR-20 */
    @PostMapping("/admin/members/{memberId}/release")
    public String release(@PathVariable Long memberId, @AuthenticationPrincipal LoginMember loginMember,
                          RedirectAttributes redirectAttributes) {
        return run(memberId, redirectAttributes, "정지를 해제했습니다",
                () -> adminMemberService.release(loginMember.getMemberId(), memberId));
    }

    /* 없는 회원(USER_NOT_FOUND)은 오류 화면, 나머지 규칙 위반은 상세 화면 + toast */
    private String run(Long memberId, RedirectAttributes redirectAttributes, String done, Runnable action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("toast", done);
        } catch (BusinessException e) {
            if (e.getErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                throw e;
            }
            redirectAttributes.addFlashAttribute("toast", e.getMessage());
        }
        return detailUrl(memberId);
    }

    private String detailUrl(Long memberId) {
        return "redirect:/admin/members/" + memberId;
    }
}
