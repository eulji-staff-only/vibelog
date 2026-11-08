package com.vibelog.domain.auth.model.dto;

import com.vibelog.domain.auth.model.entity.SuspensionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 이용 정지 (API-USR-19 form level·reason, A-08 사유·기간 모달). 사유는 필수 (INVALID_INPUT).
 * level 에 없는 값이 오면 바인딩 오류로 같은 검증 실패가 된다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SuspendRequest {

    public static final String REQUIRED = "제재 단계와 사유를 입력해 주세요";

    @NotNull(message = REQUIRED)
    private SuspensionLevel level;

    @NotBlank(message = REQUIRED)
    private String reason;
}
