package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.SignupRequest;
import com.vibelog.domain.auth.model.service.EmailVerifyService;
import com.vibelog.domain.auth.model.service.MemberSignupService;
import com.vibelog.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/*
 * [AuthController]
 * 역할 : 회원(auth) 화면 요청을 받는다.
 * 책임 : - 회원가입(U-02)·로그인(U-03)·이메일 인증 결과(U-04)·비밀번호 찾기(U-05)·재설정(U-06) 화면을 보여 준다.
 *        - 회원가입(API-USR-03)을 받아 가입한 회원을 바로 로그인 상태로 만든다.
 *        - 로그인 처리(POST /login)·로그아웃(POST /logout)은 Spring Security 가 한다 (SecurityConfig).
 *        - 이메일 인증 링크(API-USR-09)를 처리해 성공·만료·다시 받기 상태 중 하나를 보여 준다 (#89).
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final MemberSignupService memberSignupService;
    private final EmailVerifyService emailVerifyService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /* API-USR-02. 로그인한 회원이 가입 완료(done) 없이 오면 메인으로 */
    @GetMapping("/signup")
    public String signupPage(@RequestParam(required = false) String done,
                             Principal principal, Model model) {
        if (principal != null && done == null) {
            return "redirect:/";
        }
        model.addAttribute("signupRequest", new SignupRequest());
        return "auth/signup";
    }

    /*
     * API-USR-03. 실패하면 같은 폼을 입력값과 칸별 메시지로 다시 보여 준다 (방식 ④).
     * 중복·금칙어는 Service 가 BusinessException 으로 던지므로, 여기서만 받아서 해당 칸의 메시지로 바꾼다.
     */
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupRequest signupRequest, BindingResult bindingResult,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }
        LoginMember member;
        try {
            member = memberSignupService.signup(signupRequest);
        } catch (BusinessException e) {
            boolean emailError = e.getErrorCode() == AuthErrorCode.DUPLICATE_EMAIL;
            bindingResult.rejectValue(emailError ? "email" : "nickname", e.getErrorCode().toString(), e.getMessage());
            model.addAttribute("duplicateEmail", emailError);
            return "auth/signup";
        }
        logIn(member, request, response);
        return "redirect:/signup?done=true";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /*
     * API-USR-09. state: verified(인증됨) / expired(만료·사용한 링크) / resend(token 없이 열었을 때, 다시 받기).
     * 만료·사용한 링크는 오류 화면이 아니라 이 화면 안 상태로 보여 준다 (7장 TOKEN_EXPIRED·TOKEN_USED "화면 안 상태").
     */
    @GetMapping("/auth/verify")
    public String verifyPage(@RequestParam(required = false) String token, Model model) {
        if (token == null) {
            model.addAttribute("state", "resend");
            return "auth/verify";
        }
        try {
            emailVerifyService.verify(token);
            model.addAttribute("state", "verified");
        } catch (BusinessException e) {
            if (e.getErrorCode() != AuthErrorCode.TOKEN_EXPIRED && e.getErrorCode() != AuthErrorCode.TOKEN_USED) {
                throw e;
            }
            model.addAttribute("state", "expired");
        }
        return "auth/verify";
    }

    /* 폼 로그인과 같게: 비밀번호 해시를 지우고, 세션 ID 를 새로 발급하고(세션 고정 공격 방지), 로그인 정보를 세션에 저장한다 */
    private void logIn(LoginMember member, HttpServletRequest request, HttpServletResponse response) {
        member.eraseCredentials();
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(member, null, member.getAuthorities()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
