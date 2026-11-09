package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.PortfolioSubmitForm;
import com.vibelog.domain.portfolio.model.service.PortfolioService;
import com.vibelog.domain.portfolio.model.service.PortfolioSubmitService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [PortfolioPageController]
 * 역할 : 포트폴리오 영역의 화면을 연다. 과제 화면(API-PRT-01)·설정 파일 내려받기(API-PRT-03)·제출(API-PRT-04)을 받는다.
 * 책임 : - 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 수강하지 않은 코스면 코스 소개(/courses/{courseId})로 보낸다 (명세 실패 칸).
 *        - 제출 입력 오류(검증·금칙어·파일)는 같은 폼 다시 + 칸별 메시지(_template.md 7-1 ④). 이미 제출함은 과제 화면 + toast,
 *          잠김·미인증처럼 폼으로 고칠 수 없는 것은 오류 화면으로 보낸다 (CommunityPageController 와 같은 방식).
 *        - U-19 내 포트폴리오·U-20 상세는 아직 화면 이름만 돌려준다 (화면 선행 작업 #58). 데이터·수정은 #114 에서 붙인다.
 */
@Controller
@RequiredArgsConstructor
public class PortfolioPageController {

    private static final String VIEW = "portfolio/portfolio";

    private final PortfolioService portfolioService;
    private final PortfolioSubmitService portfolioSubmitService;

    /* U-17 포트폴리오 (과제 → 평가 에이전트 자가 점검 → 제출) */
    @GetMapping("/courses/{courseId}/portfolio")
    public String portfolio(@PathVariable Long courseId,
                            @AuthenticationPrincipal LoginMember loginMember,
                            Model model) {
        model.addAttribute("portfolioSubmitForm", new PortfolioSubmitForm());
        return pageView(courseId, loginMember.getMemberId(), model);
    }

    /* API-PRT-04 제출 → U-18 코스 완료 */
    @PostMapping("/courses/{courseId}/portfolio")
    public String submit(@PathVariable Long courseId,
                         @Valid @ModelAttribute PortfolioSubmitForm portfolioSubmitForm, BindingResult bindingResult,
                         @RequestParam(required = false) MultipartFile screenshot,
                         @AuthenticationPrincipal LoginMember loginMember,
                         Model model, RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        if (bindingResult.hasErrors()) {
            return pageView(courseId, memberId, model);
        }
        try {
            portfolioSubmitService.submit(memberId, courseId, portfolioSubmitForm, screenshot);
            return "redirect:/courses/" + courseId + "/complete";
        } catch (BusinessException e) {
            if (e.getErrorCode() == PortfolioErrorCode.ALREADY_SUBMITTED) {
                redirectAttributes.addFlashAttribute("toast", e.getMessage());
                return "redirect:/courses/" + courseId + "/portfolio";
            }
            if (e.getErrorCode() != CommonErrorCode.BANNED_WORD && e.getErrorCode() != CommonErrorCode.INVALID_FILE) {
                throw e;   // 잠김·미인증 등 폼으로 고칠 수 없는 것
            }
            bindingResult.reject(e.getErrorCode().toString(), e.getMessage());
            return pageView(courseId, memberId, model);
        }
    }

    /* 수강하지 않은 코스면 코스 소개로 */
    private String pageView(Long courseId, Long memberId, Model model) {
        return portfolioService.getPage(memberId, courseId)
                .map(page -> {
                    model.addAttribute("page", page);
                    return VIEW;
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
