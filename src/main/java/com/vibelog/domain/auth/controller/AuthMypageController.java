package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.MypageInfo;
import com.vibelog.domain.auth.model.dto.NicknameChangeRequest;
import com.vibelog.domain.auth.model.dto.PasswordChangeRequest;
import com.vibelog.domain.auth.model.service.MypageService;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.contest.model.service.ContestReader;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
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
 * 역할 : 마이페이지(U-24) 요청을 받는다 (API-USR-15~18, #116·#177). 회원만 연다 (SecurityConfig).
 * 책임 : - tab(info·posts·comments)마다 필요한 값만 읽는다. 작성한 글·댓글은 community 의 CommunityReader 로 받는다.
 *          배지(P2, FR-RWD-002)는 contest 의 ContestReader 로 받는다.
 *        - 닉네임 저장·비밀번호 변경 입력 오류는 같은 화면 info 탭에 칸별 메시지로 다시 보여 준다 (방식 ④).
 *        - 탈퇴하면 로그아웃(로그인 유지 쿠키도 지움)하고 메인으로 간다.
 */
@Controller
@RequiredArgsConstructor
public class AuthMypageController {

    private static final String VIEW = "auth/mypage";

    private final MypageService mypageService;
    private final CommunityReader communityReader;
    private final ContestReader contestReader;

    /* API-USR-15 */
    @GetMapping("/mypage")
    public String mypage(@RequestParam(defaultValue = "info") String tab, @RequestParam(defaultValue = "1") int page,
                         @AuthenticationPrincipal LoginMember loginMember, Model model) {
        Long memberId = loginMember.getMemberId();
        switch (tab) {
            case "favorites" -> model.addAttribute("favorites", communityReader.getFavoritePostsOf(memberId, page));
            case "posts" -> model.addAttribute("posts", communityReader.getPostsOf(memberId, page));
            case "comments" -> model.addAttribute("comments", communityReader.getCommentsOf(memberId, page));
            case "badges" -> model.addAttribute("badges", contestReader.findBadgesOf(memberId));
            default -> tab = "info";
        }
        return view(tab, memberId, model);
    }

    /* API-USR-18 */
    @PostMapping("/mypage/profile")
    public String changeNickname(@Valid @ModelAttribute NicknameChangeRequest nicknameChangeRequest, BindingResult bindingResult,
                                 @AuthenticationPrincipal LoginMember loginMember, Model model,
                                 RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        if (bindingResult.hasErrors()) {
            return view("info", memberId, model);
        }
        try {
            mypageService.changeNickname(memberId, nicknameChangeRequest.getNickname());
        } catch (BusinessException e) {
            if (e.getErrorCode() != AuthErrorCode.DUPLICATE_NICKNAME && e.getErrorCode() != CommonErrorCode.BANNED_WORD) {
                throw e;
            }
            bindingResult.rejectValue("nickname", e.getErrorCode().toString(), e.getMessage());
            return view("info", memberId, model);
        }
        redirectAttributes.addFlashAttribute("toast", "닉네임을 저장했습니다");
        return "redirect:/mypage?tab=info";
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

    /* info 탭은 두 폼을 함께 그린다. 오류로 다시 그릴 때는 보낸 폼만 모델에 있으므로 빠진 폼을 채운다 */
    private String view(String tab, Long memberId, Model model) {
        MypageInfo info = mypageService.getInfo(memberId);
        model.addAttribute("tab", tab);
        model.addAttribute("info", info);
        if ("info".equals(tab)) {
            if (!model.containsAttribute("nicknameChangeRequest")) {
                model.addAttribute("nicknameChangeRequest", new NicknameChangeRequest(info.nickname()));
            }
            if (!model.containsAttribute("passwordChangeRequest")) {
                model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
            }
        }
        return VIEW;
    }
}
