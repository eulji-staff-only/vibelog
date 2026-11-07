package com.vibelog.domain.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/*
 * 마이페이지 비밀번호 변경 (API-USR-17 form currentPassword, newPassword). 새 비밀번호 규칙은 가입과 같다 (PL-USR-001).
 * 현재 비밀번호를 같이 받아, 로그인된 화면을 다른 사람이 잠깐 쓰더라도 비밀번호를 바꾸지 못하게 한다 (#116 리뷰).
 */
@Getter
@Setter
public class PasswordChangeRequest {

    @NotBlank(message = "현재 비밀번호를 입력해 주세요")
    private String currentPassword;

    @NotNull(message = SignupRequest.PASSWORD_RULE)
    @Pattern(regexp = SignupRequest.PASSWORD_REGEX, message = SignupRequest.PASSWORD_RULE)
    private String newPassword;
}
