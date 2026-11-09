package com.vibelog.domain.contest.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.contest.model.service.ContestService;
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
 * [ContestPageController]
 * 역할 : U-32 이달의 콘테스트 (API-CTS-01)와 출품 (API-CTS-02, M-09 확인 뒤). #201
 * 책임 : 회원만 (SecurityConfig). 출품은 성공·실패 모두 /contests/current 로 돌아가 토스트로 알린다 (방식 ③).
 *        투표는 페이지 이동 없이 JSON 으로 한다 (ContestVoteApiController).
 */
@Controller
@RequiredArgsConstructor
public class ContestPageController {

    private final ContestService contestService;

    @GetMapping("/contests/current")
    public String current(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("contest", contestService.getCurrent(loginMember.getMemberId()).orElse(null));
        return "contest/current";
    }

    @PostMapping("/contests/{contestId}/entries")
    public String enter(@PathVariable Long contestId, @RequestParam Long portfolioId,
                        @AuthenticationPrincipal LoginMember loginMember, RedirectAttributes redirectAttributes) {
        try {
            contestService.enter(contestId, loginMember.getMemberId(), portfolioId);
            redirectAttributes.addFlashAttribute("toast", "출품했습니다");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("toastError", e.getMessage());
        }
        return "redirect:/contests/current";
    }
}
