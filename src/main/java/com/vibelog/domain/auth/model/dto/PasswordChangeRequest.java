package com.vibelog.domain.auth.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/* 마이페이지 비밀번호 변경 (API-USR-17 form newPassword). 규칙은 가입과 같다 (PL-USR-001) */
@Getter
@Setter
public class PasswordChangeRequest {

    @NotNull(message = SignupRequest.PASSWORD_RULE)
    @Pattern(regexp = SignupRequest.PASSWORD_REGEX, message = SignupRequest.PASSWORD_RULE)
    private String newPassword;
}
