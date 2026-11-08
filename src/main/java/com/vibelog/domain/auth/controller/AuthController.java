package com.vibelog.domain.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [AuthController]
 * 역할 : 회원(auth) 화면 요청을 받는다.
 * 책임 : - 회원가입(U-02)·로그인(U-03)·이메일 인증 결과(U-04)·비밀번호 찾기(U-05)·재설정(U-06) 화면을 보여 준다.
 *        - 로그인 처리(POST /login)·로그아웃(POST /logout)은 Spring Security 가 한다 (SecurityConfig).
 *        - 가입·인증·재설정 처리와 token 확인은 ③ 단계 기능 이슈(#88~#90)에서 붙인다. 지금은 화면 이름만 돌려준다 (#49).
 */
@Controller
public class AuthController {

    @GetMapping("/signup")
    public String signupPage() {
        return "auth/signup";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/auth/verify")
    public String verifyPage() {
        return "auth/verify";
    }

    @GetMapping("/password/find")
    public String passwordFindPage() {
        return "auth/password-find";
    }

    @GetMapping("/password/reset")
    public String passwordResetPage() {
        return "auth/password-reset";
    }
}
