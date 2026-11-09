package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.PortfolioAgentForm;
import com.vibelog.domain.portfolio.model.dto.PortfolioAssignmentForm;
import com.vibelog.domain.portfolio.model.service.PortfolioAdminService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/*
 * [PortfolioAdminController]
 * 역할 : 운영자 포트폴리오 과제 편집 A-07 (API-ADM-34~36). 코드는 portfolio 영역에 둔다 (#104).
 * 책임 : - 운영자만 들어오는 것은 SecurityConfig(/admin/**)가 막으므로 여기서 검사하지 않는다.
 *        - 입력 오류(INVALID_INPUT·INVALID_FILE)는 _template.md 7-1 ④ "같은 폼 다시 + 칸별 메시지"라서
 *          그 두 에러만 받아 폼에 붙이고, 나머지(COURSE_NOT_FOUND 등)는 GlobalExceptionHandler 에 맡긴다.
 */
@Controller
@RequiredArgsConstructor
public class PortfolioAdminController {

    private static final String VIEW = "portfolio/admin-portfolio";
    private static final String TAB_AGENT = "agent";

    private final PortfolioAdminService portfolioAdminService;

    /* API-ADM-34 과제 편집 화면. tab = assignment(기본) · agent */
    @GetMapping("/admin/courses/{courseId}/portfolio")
    public String edit(@PathVariable Long courseId,
                       @RequestParam(defaultValue = "assignment") String tab,
                       Model model) {
        model.addAttribute("assignmentForm", portfolioAdminService.getAssignmentForm(courseId));
        model.addAttribute("agentForm", portfolioAdminService.getAgentForm(courseId));
        return view(courseId, TAB_AGENT.equals(tab) ? TAB_AGENT : "assignment", model);
    }

    /* API-ADM-35 과제·스텝 저장 → A-05 코스 상세 */
    @PostMapping("/admin/courses/{courseId}/portfolio")
    public String saveAssignment(@PathVariable Long courseId,
                                 @Valid @ModelAttribute("assignmentForm") PortfolioAssignmentForm form,
                                 BindingResult bindingResult, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                portfolioAdminService.saveAssignment(courseId, form);
                return "redirect:/admin/courses/" + courseId;
            } catch (BusinessException e) {
                if (e.getErrorCode() != PortfolioErrorCode.INVALID_INPUT) {
                    throw e;
                }
                bindingResult.reject(e.getErrorCode().toString(), e.getMessage());
            }
        }
        model.addAttribute("agentForm", portfolioAdminService.getAgentForm(courseId));
        return view(courseId, "assignment", model);
    }

    /* API-ADM-36 평가 에이전트 저장 → A-07 평가 에이전트 탭 */
    @PostMapping("/admin/courses/{courseId}/portfolio/agent")
    public String saveAgent(@PathVariable Long courseId,
                            @Valid @ModelAttribute("agentForm") PortfolioAgentForm form,
                            BindingResult bindingResult,
                            @RequestParam(required = false) MultipartFile agentFile,
                            Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                portfolioAdminService.saveAgent(courseId, form.getAgentGuide(), agentFile);
                return "redirect:/admin/courses/" + courseId + "/portfolio?tab=" + TAB_AGENT;
            } catch (BusinessException e) {
                if (e.getErrorCode() != CommonErrorCode.INVALID_FILE && e.getErrorCode() != PortfolioErrorCode.INVALID_INPUT) {
                    throw e;
                }
                bindingResult.reject(e.getErrorCode().toString(), e.getMessage());
            }
        }
        model.addAttribute("assignmentForm", portfolioAdminService.getAssignmentForm(courseId));
        return view(courseId, TAB_AGENT, model);
    }

    private String view(Long courseId, String tab, Model model) {
        model.addAttribute("view", portfolioAdminService.getView(courseId));
        model.addAttribute("tab", tab);
        return VIEW;
    }
}
