package com.vibelog.domain.auth.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 마이페이지 닉네임 저장 (API-USR-18 form nickname). 규칙은 가입과 같다 (PL-USR-002).
 * 중복·금칙어는 DB 를 봐야 하므로 MypageService 가 검사한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class NicknameChangeRequest {

    @NotNull(message = SignupRequest.NICKNAME_RULE)
    @Pattern(regexp = SignupRequest.NICKNAME_REGEX, message = SignupRequest.NICKNAME_RULE)
    private String nickname;

    public NicknameChangeRequest(String nickname) {
        this.nickname = nickname;
    }
}
