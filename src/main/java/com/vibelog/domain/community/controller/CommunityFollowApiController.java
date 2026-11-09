package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.model.dto.FollowResult;
import com.vibelog.domain.community.model.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [CommunityFollowApiController]
 * 역할 : 팔로우·취소 (API-CMT-15, #181). 화면이 바뀌지 않으므로 JSON 으로 답한다 (D-08).
 */
@RestController
@RequiredArgsConstructor
public class CommunityFollowApiController {

    private final ProfileService profileService;

    @PostMapping("/api/users/{memberId}/follow")
    public FollowResult toggle(@PathVariable Long memberId, @AuthenticationPrincipal LoginMember loginMember) {
        return profileService.toggleFollow(loginMember.getMemberId(), memberId);
    }
}
