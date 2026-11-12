package com.vibelog.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/* 테스트 전용 컨트롤러 (src/test 에만 있고, src/main 에는 추가하지 않는다).
   common/admin-layout.html :: sidebar fragment 가 README 예시대로 붙는지 확인하는 빈 화면만 보여준다.
   실제 /admin 화면(A-01~A-08)의 Controller 는 다른 이슈(#120 등) 범위다 (#81 AdminLayoutFragmentTest 용). */
@Controller
public class AdminTestSampleController {

    @GetMapping("/admintest/sample")
    public String sample(@RequestParam(required = false) String active, Model model) {
        model.addAttribute("active", active);
        return "admintest/sample";
    }
}
