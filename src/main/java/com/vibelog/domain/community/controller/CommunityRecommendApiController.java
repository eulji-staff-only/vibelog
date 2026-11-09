package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.model.dto.RecommendResult;
import com.vibelog.domain.community.model.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [CommunityRecommendApiController]
 * 역할 : 게시글 상세(U-22)의 추천 버튼이 부르는 JSON (API-CMT-11). 페이지 이동 없이 숫자를 바꾼다 (FR-CMT-005).
 * 책임 : 회원만 (SecurityConfig). 작품 공유 글·내 글 검사는 RecommendService 가 한다.
 */
@RestController
@RequiredArgsConstructor
public class CommunityRecommendApiController {

    private final RecommendService recommendService;

    @PostMapping("/api/posts/{postId}/recommend")
    public RecommendResult toggle(@PathVariable Long postId, @AuthenticationPrincipal LoginMember loginMember) {
        return recommendService.toggle(loginMember.getMemberId(), postId);
    }
}
