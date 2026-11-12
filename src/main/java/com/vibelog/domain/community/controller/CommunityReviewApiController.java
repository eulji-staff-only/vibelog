package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.model.dto.HelpfulResult;
import com.vibelog.domain.community.model.service.PeerReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [CommunityReviewApiController]
 * 역할 : U-20 받은 리뷰의 "도움됨" 버튼이 부르는 JSON (API-REV-06, FR-REV-004).
 * 책임 : 요청자(작품 주인)만 켜고 끈다 (NOT_OWNER, PL-REV-003).
 */
@RestController
@RequiredArgsConstructor
public class CommunityReviewApiController {

    private final PeerReviewService peerReviewService;

    @PostMapping("/api/reviews/{peerReviewId}/helpful")
    public HelpfulResult toggle(@PathVariable Long peerReviewId, @AuthenticationPrincipal LoginMember loginMember) {
        return new HelpfulResult(peerReviewService.toggleHelpful(loginMember.getMemberId(), peerReviewId));
    }
}
