package com.vibelog.domain.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/*
 * [SignupRequest]
 * 역할 : 회원가입 폼 값 (API-USR-03 form nickname, email, password).
 * 책임 : - 입력 규칙(PL-USR-001·002)은 여기서 검사한다. 중복·금칙어는 DB 를 봐야 하므로 MemberSignupService 가 검사한다.
 *        - 비밀번호는 BCrypt 가 72바이트까지만 쓰므로 영문·숫자·특수문자로 64자까지만 받는다 (한글·공백 제외).
 */
@Getter
@Setter
public class SignupRequest {

    private static final String NICKNAME_RULE = "닉네임은 2~12자 한글·영문·숫자로 써 주세요 (공백·특수문자 불가)";
    private static final String PASSWORD_RULE = "비밀번호는 8~64자, 영문과 숫자를 모두 넣어 주세요 (한글·공백 제외)";

    @NotNull(message = NICKNAME_RULE)
    @Pattern(regexp = "[가-힣A-Za-z0-9]{2,12}", message = NICKNAME_RULE)
    private String nickname;

    @NotBlank(message = "이메일을 입력해 주세요")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Size(max = 255, message = "이메일은 255자까지 쓸 수 있습니다")
    private String email;

    @NotNull(message = PASSWORD_RULE)
    @Pattern(regexp = "(?=.*[A-Za-z])(?=.*[0-9])[\\x21-\\x7E]{8,64}", message = PASSWORD_RULE)
    private String password;
}
