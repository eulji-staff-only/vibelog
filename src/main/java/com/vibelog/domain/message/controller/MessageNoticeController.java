package com.vibelog.domain.message.controller;

import com.vibelog.domain.message.model.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * [MessageNoticeController]
 * 역할 : 공지 목록(U-25, API-NTF-04)·상세(U-26, API-NTF-05) 화면.
 * 책임 : 회원만 (SecurityConfig, 비회원은 로그인으로). 없는 공지는 NOTICE_NOT_FOUND → 오류 화면.
 */
@Controller
@RequiredArgsConstructor
public class MessageNoticeController {

    private final NoticeService noticeService;

    @GetMapping("/notices")
    public String list(@RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("notices", noticeService.getPage(page));
        return "message/notice-list";
    }

    @GetMapping("/notices/{noticeId}")
    public String detail(@PathVariable Long noticeId, Model model) {
        model.addAttribute("notice", noticeService.get(noticeId));
        return "message/notice-detail";
    }
}
