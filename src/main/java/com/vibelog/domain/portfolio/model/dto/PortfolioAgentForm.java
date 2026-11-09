package com.vibelog.domain.portfolio.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * [PortfolioAgentForm]
 * 역할 : 평가 에이전트 저장 폼 (API-ADM-36).
 * 책임 : - 설정 파일(agentFile)은 MultipartFile 이라 Controller 가 따로 받는다.
 *        - 안내에는 세팅 방법·평가 기준·실행 방법·"실행이 안 될 때" 체크리스트를 쓴다 (FR-PRT-013).
 */
@Getter
@Setter
@NoArgsConstructor
public class PortfolioAgentForm {

    @NotBlank(message = "평가 에이전트 안내를 입력해 주세요.")
    private String agentGuide;

    public PortfolioAgentForm(String agentGuide) {
        this.agentGuide = agentGuide;
    }
}
