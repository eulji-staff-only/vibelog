package com.vibelog.domain.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/* 인증 메일 다시 받기 (API-USR-10 json email). 로그인 상태면 보내지 않아도 된다 */
public record VerificationMailRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 255, message = "이메일은 255자까지 쓸 수 있습니다")
        String email) {
}
