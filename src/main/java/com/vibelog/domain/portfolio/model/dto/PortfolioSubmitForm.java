package com.vibelog.domain.portfolio.model.dto;

import com.vibelog.domain.portfolio.model.entity.Portfolio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * [PortfolioSubmitForm]
 * 역할 : 포트폴리오 제출 폼 (API-PRT-04). 스크린샷(screenshot)은 MultipartFile 이라 Controller 가 따로 받는다.
 * 책임 : - 필수·선택 항목은 PL-PRT-002, 길이는 PL-PRT-003, 링크는 PL-PRT-004 (http·https URL 형식, 접속 여부는 보지 않음).
 *        - 공개 여부는 기본값이 없어 고르지 않으면 오류다 (PL-PRT-006).
 *        - 명세에 없는 길이(결과물 링크·AI 도구)는 sql/01-schema.sql 컬럼 크기 (result_url 500, ai_tools 200).
 *        - 틀리면 U-17 의 같은 폼을 입력 값 그대로 다시 그리려고 setter 가 있는 클래스로 둔다 (Entity 가 아님).
 *        - 수정(API-PRT-06)도 같은 폼이다. U-17 수정 상태는 from(저장된 포트폴리오)으로 채운다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PortfolioSubmitForm {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 50, message = "제목은 50자까지 입력할 수 있습니다.")
    private String title;

    @NotBlank(message = "올바른 URL을 입력하세요")
    @Size(max = 500, message = "결과물 링크는 500자까지 입력할 수 있습니다.")
    @Pattern(regexp = "^https?://\\S+\\.\\S+$", message = "올바른 URL을 입력하세요")
    private String resultUrl;

    @NotBlank(message = "한 줄 요약을 입력해 주세요.")
    @Size(max = 100, message = "한 줄 요약은 100자까지 입력할 수 있습니다.")
    private String summary;

    @NotBlank(message = "회고를 입력해 주세요.")
    @Size(min = 100, max = 2000, message = "회고는 100자 이상 2,000자 이하로 입력해 주세요.")
    private String retrospective;

    @NotBlank(message = "사용한 AI 도구·모델을 입력해 주세요.")
    @Size(max = 200, message = "사용한 AI 도구·모델은 200자까지 입력할 수 있습니다.")
    private String aiTools;

    @NotNull(message = "공개 여부를 골라 주세요.")
    private Boolean isPublic;

    private String agentResult;

    /* 수정(API-PRT-06)에서 올라가 있는 스크린샷을 지우려고 보낸다. 새 스크린샷이 같이 오면 새 파일이 우선이다 (#437) */
    private boolean removeScreenshot;

    /* U-17 수정 상태(edit=true)를 저장된 값으로 채운다 */
    public static PortfolioSubmitForm from(Portfolio portfolio) {
        PortfolioSubmitForm form = new PortfolioSubmitForm();
        form.title = portfolio.getTitle();
        form.resultUrl = portfolio.getResultUrl();
        form.summary = portfolio.getSummary();
        form.retrospective = portfolio.getRetrospective();
        form.aiTools = portfolio.getAiTools();
        form.isPublic = portfolio.isPublic();
        form.agentResult = portfolio.getAgentResult();
        return form;
    }
}
