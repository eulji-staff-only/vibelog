package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.model.service.ReportAdminService;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [MessageAdminReportController]
 * 역할 : 백오피스 신고 처리 A-13 (API-RPT-02~04, FR-RPT-004, #257).
 * 책임 : - 운영자만 (SecurityConfig /admin/**). reportId 가 있으면 오른쪽에 그 대상의 원문·작성자 이력·신고 목록.
 *        - REPORT_NOT_FOUND·REPORT_ALREADY_HANDLED 는 목록으로 돌아가 토스트 (방식 ③, _template.md 7장).
 */
@Controller
@RequiredArgsConstructor
public class MessageAdminReportController {

    private static final String LIST = "redirect:/admin/reports";

    private final ReportAdminService reportAdminService;

    @GetMapping("/admin/reports")
    public String manage(@RequestParam(defaultValue = "1") int page, @RequestParam(required = false) Long reportId,
                         Model model, RedirectAttributes redirectAttributes) {
        if (reportId != null) {
            try {
                model.addAttribute("detail", reportAdminService.getDetail(reportId));
            } catch (BusinessException e) {
                redirectAttributes.addFlashAttribute("toast", e.getMessage());
                return LIST;
            }
        }
        model.addAttribute("reports", reportAdminService.getPendingTargets(page));
        return "message/admin-reports";
    }

    @PostMapping("/admin/reports/{reportId}/hide")
    public String hide(@PathVariable Long reportId, @AuthenticationPrincipal LoginMember loginMember, RedirectAttributes redirectAttributes) {
        return run(redirectAttributes, "숨김 처리했습니다", () -> reportAdminService.hide(loginMember.getMemberId(), reportId));
    }

    @PostMapping("/admin/reports/{reportId}/dismiss")
    public String dismiss(@PathVariable Long reportId, @AuthenticationPrincipal LoginMember loginMember, RedirectAttributes redirectAttributes) {
        return run(redirectAttributes, "기각했습니다", () -> reportAdminService.dismiss(loginMember.getMemberId(), reportId));
    }

    private String run(RedirectAttributes redirectAttributes, String done, Runnable action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("toast", done);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("toast", e.getMessage());
        }
        return LIST;
    }
}
