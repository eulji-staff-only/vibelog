package com.vibelog.domain.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/* 재설정 메일 보내기 폼 (API-USR-12 form email) */
@Getter
@Setter
public class PasswordFindRequest {

    @NotBlank(message = "이메일을 입력해 주세요")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Size(max = 255, message = "이메일은 255자까지 쓸 수 있습니다")
    private String email;
}
