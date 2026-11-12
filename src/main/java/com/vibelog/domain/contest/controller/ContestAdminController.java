package com.vibelog.domain.contest.controller;

import com.vibelog.domain.contest.model.dto.ContestForm;
import com.vibelog.domain.contest.model.service.ContestAdminService;
import com.vibelog.global.exception.BusinessException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [ContestAdminController]
 * 역할 : 백오피스 콘테스트 관리 (A-11, API-CTS-06~08, #200). 위는 등록·수정 폼, 아래는 콘테스트 목록.
 * 책임 : 운영자만 (SecurityConfig /admin/**). 입력 오류와 월 중복·출품 시작 뒤 일정 변경은 같은 화면에 다시 보여 준다 (방식 ④).
 *        없는 콘테스트는 CONTEST_NOT_FOUND → 오류 화면.
 */
@Controller
@RequiredArgsConstructor
public class ContestAdminController {

    private static final String VIEW = "contest/admin-contests";

    private final ContestAdminService contestAdminService;

    @GetMapping("/admin/contests")
    public String manage(@RequestParam(defaultValue = "1") int page, @RequestParam(required = false) Long contestId, Model model) {
        model.addAttribute("contestForm", contestId == null ? contestAdminService.newForm() : contestAdminService.getForm(contestId));
        return view(page, contestId, model);
    }

    @PostMapping("/admin/contests")
    public String create(@Valid @ModelAttribute ContestForm contestForm, BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                contestAdminService.create(contestForm);
                redirectAttributes.addFlashAttribute("toast", "콘테스트를 등록했습니다");
                return "redirect:/admin/contests";
            } catch (BusinessException e) {
                bindingResult.reject(e.getErrorCode().name(), e.getMessage());
            }
        }
        return view(1, null, model);
    }

    @PostMapping("/admin/contests/{contestId}/edit")
    public String update(@PathVariable Long contestId, @Valid @ModelAttribute ContestForm contestForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                contestAdminService.update(contestId, contestForm);
                redirectAttributes.addFlashAttribute("toast", "콘테스트를 수정했습니다");
                return "redirect:/admin/contests";
            } catch (BusinessException e) {
                bindingResult.reject(e.getErrorCode().name(), e.getMessage());
            }
        }
        return view(1, contestId, model);
    }

    private String view(int page, Long contestId, Model model) {
        model.addAttribute("contests", contestAdminService.getPage(page));
        model.addAttribute("editingId", contestId);
        return VIEW;
    }
}
