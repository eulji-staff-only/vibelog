package com.vibelog.domain.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [PortfolioPageController]
 * 역할 : 포트폴리오 영역의 화면(GET)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #57·#58)
 *        - 과제·스텝 체크·제출·목록·상세 데이터는 ③ 단계에서 API 명세(API-PRT-01~09)대로 붙인다.
 */
@Controller
public class PortfolioPageController {

    /* U-17 포트폴리오 (과제 → 평가 에이전트 자가 점검 → 제출) */
    @GetMapping("/courses/{courseId}/portfolio")
    public String portfolio() {
        return "portfolio/portfolio";
    }

    /* U-19 내 포트폴리오 */
    @GetMapping("/my/portfolios")
    public String myPortfolios() {
        return "portfolio/my-portfolios";
    }

    /* U-20 포트폴리오 상세 */
    @GetMapping("/portfolios/{portfolioId}")
    public String detail() {
        return "portfolio/detail";
    }
}
