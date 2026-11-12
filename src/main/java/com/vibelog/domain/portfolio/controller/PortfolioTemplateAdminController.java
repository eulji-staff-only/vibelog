package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.PortfolioTemplateForm;
import com.vibelog.domain.portfolio.model.service.PortfolioTemplateService;
import com.vibelog.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [PortfolioTemplateAdminController]
 * 역할 : 운영자 템플릿 관리 A-12 (API-PRT-14·15, FR-PRT-015, #258).
 * 책임 : - 운영자만 들어오는 것은 SecurityConfig(/admin/**)가 막으므로 여기서 검사하지 않는다.
 *        - 입력 오류(검증·INVALID_INPUT)는 _template.md 7-1 ④ "같은 폼 다시 + 메시지". 저장하면 A-12 + toast.
 */
@Controller
@RequiredArgsConstructor
public class PortfolioTemplateAdminController {

    private static final String VIEW = "portfolio/admin-templates";

    private final PortfolioTemplateService portfolioTemplateService;

    /* API-PRT-14 */
    @GetMapping("/admin/templates")
    public String templates(Model model) {
        model.addAttribute("templateForm", PortfolioTemplateForm.of(portfolioTemplateService.getItems()));
        return VIEW;
    }

    /* API-PRT-15 → A-12 + toast */
    @PostMapping("/admin/templates")
    public String save(@Valid @ModelAttribute("templateForm") PortfolioTemplateForm form, BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                portfolioTemplateService.save(form);
                redirectAttributes.addFlashAttribute("toast", "템플릿을 저장했습니다");
                return "redirect:/admin/templates";
            } catch (BusinessException e) {
                if (e.getErrorCode() != PortfolioErrorCode.INVALID_INPUT) {
                    throw e;
                }
                bindingResult.reject(e.getErrorCode().toString(), e.getMessage());
            }
        }
        return VIEW;
    }
}
