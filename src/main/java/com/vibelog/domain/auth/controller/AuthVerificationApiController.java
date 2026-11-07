package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.VerificationMailRequest;
import com.vibelog.domain.auth.model.service.EmailVerifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/*
 * [AuthVerificationApiController]
 * 역할 : 인증 메일 다시 받기 (API-USR-10). 화면이 바뀌지 않으므로 JSON 으로 답한다 (D-08).
 * 책임 : - 로그인 상태면 로그인 회원의 이메일로, 아니면 받은 email 로 보낸다.
 *        - 성공하면 204 (명세에 돌려줄 값이 없다). 실패는 GlobalExceptionHandler 가 {code, message} 로 보낸다
 *          (RESEND_TOO_SOON 429·ALREADY_VERIFIED 409 는 로그인한 본인에게만, INVALID_INPUT 400).
 */
@RestController
@RequiredArgsConstructor
public class AuthVerificationApiController {

    private final EmailVerifyService emailVerifyService;

    @PostMapping("/api/auth/verification-mail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend(@Valid @RequestBody(required = false) VerificationMailRequest request,
                       @AuthenticationPrincipal LoginMember loginMember) {
        String email = loginMember != null ? loginMember.getUsername() : request == null ? null : request.email();
        if (email != null && !email.isBlank()) {
            emailVerifyService.resend(email.strip(), loginMember != null);
        }
    }
}
