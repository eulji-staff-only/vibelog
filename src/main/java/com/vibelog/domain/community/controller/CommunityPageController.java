package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.model.entity.PostCategory;
import com.vibelog.domain.community.model.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * [CommunityPageController]
 * 역할 : 커뮤니티 화면 요청을 받는다.
 * 책임 : - 게시판 목록(U-21, API-CMT-01)·게시글 상세(U-22, API-CMT-02)를 보여 준다. 비회원도 연다 (PL-USR-008).
 *        - 글쓰기·수정(U-23)은 회원만 연다 (SecurityConfig). 저장은 #93 에서 붙인다.
 */
@Controller
@RequiredArgsConstructor
public class CommunityPageController {

    private final PostService postService;

    @GetMapping("/community")
    public String listPage(@RequestParam(required = false) PostCategory category,
                           @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("posts", postService.getPosts(category, q, page));
        model.addAttribute("category", category);
        model.addAttribute("q", q == null ? "" : q.strip());
        model.addAttribute("categories", PostCategory.values());
        return "community/list";
    }

    @GetMapping("/community/{postId}")
    public String detailPage(@PathVariable Long postId, @AuthenticationPrincipal LoginMember loginMember, Model model) {
        Long loginMemberId = loginMember == null ? null : loginMember.getMemberId();
        model.addAttribute("post", postService.getPost(postId, loginMemberId));
        return "community/detail";
    }

    @GetMapping({"/community/write", "/community/{postId}/edit"})
    public String writePage() {
        return "community/write";
    }
}
