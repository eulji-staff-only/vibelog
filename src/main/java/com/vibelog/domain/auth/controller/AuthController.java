package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.SignupRequest;
import com.vibelog.domain.auth.model.policy.LoginLockPolicy;
import com.vibelog.domain.auth.model.policy.SocialLoginRegistrations;
import com.vibelog.domain.auth.model.policy.SocialLoginSuccessHandler;
import com.vibelog.domain.auth.model.service.EmailVerifyService;
import com.vibelog.domain.auth.model.service.MemberSignupService;
import com.vibelog.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final SocialLoginRegistrations socialLoginRegistrations;
    private final LoginLockPolicy loginLockPolicy;
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

    /*
     * API-USR-04. 구글·깃허브 버튼은 키가 있는 것만 보여 준다 (#91).
     * 소셜 로그인은 구글·깃허브를 돌아오며 redirect 값을 잃으므로 세션에 두고, 돌아오면 SocialLoginSuccessHandler 가 꺼낸다 (#363).
     * redirect 없이 열면 지난 값을 지운다 (예전에 열어 둔 로그인 화면의 주소로 가지 않게).
     * 로그인한 회원이 오면 회원가입(API-USR-02)과 같게 메인으로 보낸다. 로그인 뒤 뒤로 가기도 이 요청을 다시 보내므로 함께 막힌다 (#393).
     * 이때 /login 으로 온 안내(toast, 예: 비밀번호 재설정 완료)는 메인에서 보이게 넘긴다.
     * 잠금(locked)이면 풀리는 시각(until)으로 남은 초를 계산해 넘긴다. 화면은 그 값에서 1초씩 줄인다 (#394).
     * until 은 주소에 있어 바꿀 수 있어서 계산(5분을 넘지 않게 자르기 포함)은 LoginLockPolicy 가 한다.
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect, @RequestParam(required = false) Long until,
                            Principal principal, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (principal != null) {
            if (model.getAttribute("toast") != null) {
                redirectAttributes.addFlashAttribute("toast", model.getAttribute("toast"));
            }
            return "redirect:/";
        }
        if (until != null) {
            model.addAttribute("lockRemainingSeconds", loginLockPolicy.remainingSeconds(until));
        }
        if (redirect == null) {
            session.removeAttribute(SocialLoginSuccessHandler.REDIRECT);
        } else {
            session.setAttribute(SocialLoginSuccessHandler.REDIRECT, redirect);
        }
        model.addAttribute("socialProviders", socialLoginRegistrations.enabledIds());
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
