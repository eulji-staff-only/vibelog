package com.vibelog.domain.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [AuthController]
 * 역할 : 회원(auth) 화면 요청을 받는다.
 * 책임 : 로그인 화면(U-03)을 보여 준다. 로그인 처리(POST /login)·로그아웃(POST /logout)은 Spring Security 가 한다 (SecurityConfig).
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}
