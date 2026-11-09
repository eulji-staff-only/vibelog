package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.model.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * [CommunityProfileController]
 * 역할 : 공개 프로필 화면 (U-29, API-CMT-14, #181). 회원만 연다 (SecurityConfig, 화면 흐름 1-1).
 */
@Controller
@RequiredArgsConstructor
public class CommunityProfileController {

    private final ProfileService profileService;

    @GetMapping("/users/{memberId}")
    public String profile(@PathVariable Long memberId, @RequestParam(defaultValue = "1") int page,
                          @AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("profile", profileService.getProfile(memberId, loginMember.getMemberId(), page));
        return "community/profile";
    }
}
