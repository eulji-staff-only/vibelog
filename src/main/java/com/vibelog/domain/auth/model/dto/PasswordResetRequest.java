package com.vibelog.domain.auth.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/*
 * 비밀번호 재설정 폼 (API-USR-14 form token, newPassword, newPasswordConfirm).
 * 새 비밀번호 규칙은 가입과 같다 (PL-USR-001). 확인이 다르면 newPasswordConfirm 칸에 메시지를 보여 준다.
 */
@Getter
@Setter
public class PasswordResetRequest {

    private String token;

    @NotNull(message = SignupRequest.PASSWORD_RULE)
    @Pattern(regexp = SignupRequest.PASSWORD_REGEX, message = SignupRequest.PASSWORD_RULE)
    private String newPassword;

    private String newPasswordConfirm;

    @AssertTrue(message = "새 비밀번호가 서로 다릅니다")
    public boolean isNewPasswordConfirmed() {
        return Objects.equals(newPassword, newPasswordConfirm);
    }
}
