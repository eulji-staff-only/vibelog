package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.portfolio.model.service.PortfolioService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
 * [PortfolioPageController]
 * 역할 : 포트폴리오 영역의 화면(GET)을 연다. 과제 화면(API-PRT-01)과 설정 파일 내려받기(API-PRT-03)는 데이터를 붙였다.
 * 책임 : - 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 수강하지 않은 코스면 코스 소개(/courses/{courseId})로 보낸다 (명세 실패 칸).
 *        - U-19 내 포트폴리오·U-20 상세는 아직 화면 이름만 돌려준다 (화면 선행 작업 #58). 데이터·제출·수정은 #113·#114 에서 붙인다.
 */
@Controller
@RequiredArgsConstructor
public class PortfolioPageController {

    private final PortfolioService portfolioService;

    /* U-17 포트폴리오 (과제 → 평가 에이전트 자가 점검 → 제출) */
    @GetMapping("/courses/{courseId}/portfolio")
    public String portfolio(@PathVariable Long courseId,
                            @AuthenticationPrincipal LoginMember loginMember,
                            Model model) {
        return portfolioService.getPage(loginMember.getMemberId(), courseId)
                .map(page -> {
                    model.addAttribute("page", page);
                    return "portfolio/portfolio";
                })
                .orElse("redirect:/courses/" + courseId);
    }

    /*
     * API-PRT-03 설정 파일 내려받기. 과제가 열렸는지 확인한 뒤, 저장된 파일(/uploads/...)을 이 주소 그대로 응답한다.
     * 파일을 읽어 보내는 일은 FileStorage 가 등록한 /uploads/** 처리기에 맡기고(forward), 여기서는 "내려받기" 헤더만 붙인다.
     */
    @GetMapping("/courses/{courseId}/portfolio/agent-file")
    public String agentFile(@PathVariable Long courseId,
                            @AuthenticationPrincipal LoginMember loginMember,
                            HttpServletResponse response) {
        String fileUrl = portfolioService.getAgentFileUrl(loginMember.getMemberId(), courseId);
        String fileName = "agent-file." + StringUtils.getFilenameExtension(fileUrl);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString());
        return "forward:" + fileUrl;
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
