package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.exception.MessageErrorCode;
import com.vibelog.domain.message.model.dto.NoticeForm;
import com.vibelog.domain.message.model.service.NoticeService;
import com.vibelog.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * [MessageAdminNoticeController]
 * 역할 : 백오피스 공지 관리(A-10): 목록과 작성·수정 폼 (API-NTF-06~08, FR-NTF-005).
 * 책임 : - 운영자만 (SecurityConfig /admin/**). noticeId 가 있으면 그 공지로 폼을 채워 수정 모드.
 *        - 입력 오류와 배너 기간 겹침(BANNER_PERIOD_OVERLAP)은 같은 화면에 입력값과 메시지를 다시 보여 준다 (방식 ④).
 */
@Controller
@RequiredArgsConstructor
public class MessageAdminNoticeController {

    private static final String VIEW = "message/admin-notices";

    private final NoticeService noticeService;

    @GetMapping("/admin/notices")
    public String manage(@RequestParam(defaultValue = "1") int page, @RequestParam(required = false) Long noticeId, Model model) {
        model.addAttribute("noticeForm", noticeId == null ? new NoticeForm() : noticeService.getForm(noticeId));
        return view(page, noticeId, model);
    }

    @PostMapping("/admin/notices")
    public String create(@Valid @ModelAttribute NoticeForm noticeForm, BindingResult bindingResult,
                         @AuthenticationPrincipal LoginMember loginMember, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return view(1, null, model);
        }
        try {
            noticeService.create(loginMember.getMemberId(), noticeForm);
        } catch (BusinessException e) {
            return overlap(e, bindingResult, 1, null, model);
        }
        redirectAttributes.addFlashAttribute("toast", "공지를 등록했습니다");
        return "redirect:/admin/notices";
    }

    @PostMapping("/admin/notices/{noticeId}/edit")
    public String update(@PathVariable Long noticeId, @Valid @ModelAttribute NoticeForm noticeForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return view(1, noticeId, model);
        }
        try {
            noticeService.update(noticeId, noticeForm);
        } catch (BusinessException e) {
            return overlap(e, bindingResult, 1, noticeId, model);
        }
        redirectAttributes.addFlashAttribute("toast", "공지를 수정했습니다");
        return "redirect:/admin/notices";
    }

    /* 배너 기간 겹침만 폼 메시지로 돌려보낸다. 나머지(없는 공지 등)는 오류 화면 */
    private String overlap(BusinessException e, BindingResult bindingResult, int page, Long noticeId, Model model) {
        if (e.getErrorCode() != MessageErrorCode.BANNER_PERIOD_OVERLAP) {
            throw e;
        }
        bindingResult.rejectValue("bannerStartsAt", e.getErrorCode().name(), e.getMessage());
        return view(page, noticeId, model);
    }

    private String view(int page, Long noticeId, Model model) {
        model.addAttribute("notices", noticeService.getPage(page));
        model.addAttribute("editingId", noticeId);
        return VIEW;
    }
}
