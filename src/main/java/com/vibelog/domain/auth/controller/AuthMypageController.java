package com.vibelog.domain.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [AuthMypageController]
 * 역할 : 마이페이지 화면 요청을 받는다.
 * 책임 : - 마이페이지(U-24) 화면을 보여 준다. 탭(info·posts·comments)은 화면이 주소의 tab 으로 고른다. 회원만 연다 (SecurityConfig).
 *        - 화면 선행 작업(#51)이라 화면 이름만 돌려준다. 계정 정보·목록·비밀번호 변경·탈퇴는 ③ 단계(#116)에서 붙인다.
 */
@Controller
public class AuthMypageController {

    @GetMapping("/mypage")
    public String mypage() {
        return "auth/mypage";
    }
}
