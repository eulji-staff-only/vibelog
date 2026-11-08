package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.PasswordChangeRequest;
import com.vibelog.domain.auth.model.service.MypageService;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.global.exception.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [AuthMypageController]
 * 역할 : 마이페이지(U-24) 요청을 받는다 (API-USR-15~17, #116). 회원만 연다 (SecurityConfig).
 * 책임 : - tab(info·posts·comments)마다 필요한 값만 읽는다. 작성한 글·댓글은 community 의 CommunityReader 로 받는다.
 *        - 비밀번호 변경 입력 오류는 같은 화면 info 탭에 칸별 메시지로 다시 보여 준다 (방식 ④).
 *        - 탈퇴하면 로그아웃(로그인 유지 쿠키도 지움)하고 메인으로 간다.
 */
@Controller
@RequiredArgsConstructor
public class AuthMypageController {

    private static final String VIEW = "auth/mypage";

    private final MypageService mypageService;
    private final CommunityReader communityReader;

    /* API-USR-15 */
    @GetMapping("/mypage")
    public String mypage(@RequestParam(defaultValue = "info") String tab, @RequestParam(defaultValue = "1") int page,
                         @AuthenticationPrincipal LoginMember loginMember, Model model) {
        Long memberId = loginMember.getMemberId();
        switch (tab) {
            case "posts" -> model.addAttribute("posts", communityReader.getPostsOf(memberId, page));
            case "comments" -> model.addAttribute("comments", communityReader.getCommentsOf(memberId, page));
            default -> {
                tab = "info";
                model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
            }
        }
        return view(tab, memberId, model);
    }

    /* API-USR-17 */
    @PostMapping("/mypage/password")
    public String changePassword(@Valid @ModelAttribute PasswordChangeRequest passwordChangeRequest, BindingResult bindingResult,
                                 @AuthenticationPrincipal LoginMember loginMember, Model model,
                                 RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        if (bindingResult.hasErrors()) {
            return view("info", memberId, model);
        }
        try {
            mypageService.changePassword(memberId, passwordChangeRequest.getCurrentPassword(), passwordChangeRequest.getNewPassword());
        } catch (BusinessException e) {
            if (e.getErrorCode() != AuthErrorCode.SOCIAL_ACCOUNT && e.getErrorCode() != AuthErrorCode.PASSWORD_MISMATCH) {
                throw e;
            }
            String field = e.getErrorCode() == AuthErrorCode.PASSWORD_MISMATCH ? "currentPassword" : "newPassword";
            bindingResult.rejectValue(field, e.getErrorCode().toString(), e.getMessage());
            return view("info", memberId, model);
        }
        redirectAttributes.addFlashAttribute("toast", "비밀번호를 변경했습니다");
        return "redirect:/mypage?tab=info";
    }

    /* API-USR-16. M-06 에서 확인한 뒤 온다 */
    @PostMapping("/mypage/withdraw")
    public String withdraw(@AuthenticationPrincipal LoginMember loginMember, HttpServletRequest request,
                           RedirectAttributes redirectAttributes) throws ServletException {
        mypageService.withdraw(loginMember.getMemberId());
        request.logout();
        redirectAttributes.addFlashAttribute("toast", "회원 탈퇴가 완료되었습니다");
        return "redirect:/";
    }

    private String view(String tab, Long memberId, Model model) {
        model.addAttribute("tab", tab);
        model.addAttribute("info", mypageService.getInfo(memberId));
        return VIEW;
    }
}
