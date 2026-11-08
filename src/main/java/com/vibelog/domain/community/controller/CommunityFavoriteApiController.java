package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.model.dto.FavoriteResult;
import com.vibelog.domain.community.model.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [CommunityFavoriteApiController]
 * 역할 : 게시글 즐겨찾기·취소 (API-CMT-13, #180). 화면이 바뀌지 않으므로 JSON 으로 답한다 (D-08).
 * 책임 : 회원만 부른다 (SecurityConfig). 실패는 GlobalExceptionHandler 가 {code, message} 로 보낸다.
 */
@RestController
@RequiredArgsConstructor
public class CommunityFavoriteApiController {

    private final FavoriteService favoriteService;

    @PostMapping("/api/posts/{postId}/favorite")
    public FavoriteResult toggle(@PathVariable Long postId, @AuthenticationPrincipal LoginMember loginMember) {
        return new FavoriteResult(favoriteService.toggle(loginMember.getMemberId(), postId));
    }
}
