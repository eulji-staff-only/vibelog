package com.vibelog.domain.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [CommunityPageController]
 * 역할 : 커뮤니티 화면 요청을 받는다.
 * 책임 : - 게시판 목록(U-21)·게시글 상세(U-22)·글쓰기·수정(U-23) 화면을 보여 준다.
 *        - 화면 선행 작업(#50)이라 화면 이름만 돌려준다. 목록·상세 데이터와 저장은 ③ 단계(#92~#94)에서 붙인다.
 *          글쓰기와 수정은 같은 화면이고, 수정이면 editing 으로 제목·버튼 문구만 바꾼다.
 *        - 글쓰기·수정은 회원만 연다 (SecurityConfig). 이메일 인증 검사는 ③ 단계 Service 가 한다 (PL-USR-003).
 */
@Controller
public class CommunityPageController {

    @GetMapping("/community")
    public String listPage() {
        return "community/list";
    }

    @GetMapping("/community/{postId}")
    public String detailPage() {
        return "community/detail";
    }

    @GetMapping("/community/write")
    public String writePage(Model model) {
        model.addAttribute("editing", false);
        return "community/write";
    }

    @GetMapping("/community/{postId}/edit")
    public String editPage(Model model) {
        model.addAttribute("editing", true);
        return "community/write";
    }
}
