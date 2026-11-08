package com.vibelog.domain.auth.controller;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dto.PasswordFindRequest;
import com.vibelog.domain.auth.model.dto.PasswordResetRequest;
import com.vibelog.domain.auth.model.service.PasswordResetService;
import com.vibelog.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [AuthPasswordController]
 * 역할 : 비밀번호 찾기(U-05)·재설정(U-06) 요청을 받는다 (API-USR-11~14, #90). 비회원도 쓴다.
 * 책임 : - 메일 보내기는 가입 여부와 관계없이 항상 "메일을 보냈습니다"(sent=true)로 간다.
 *        - 입력 오류는 같은 폼에 칸별 메시지로 다시 보여 준다 (방식 ④).
 *        - 만료·사용한 링크(TOKEN_EXPIRED·TOKEN_USED)는 오류 화면이 아니라 U-06 의 만료 상태로 보여 준다 ("화면 안 상태").
 */
@Controller
@RequiredArgsConstructor
public class AuthPasswordController {

    private static final String FIND_VIEW = "auth/password-find";
    private static final String RESET_VIEW = "auth/password-reset";

    private final PasswordResetService passwordResetService;

    /* API-USR-11. sent=true 면 "메일을 보냈습니다" 단계 */
    @GetMapping("/password/find")
    public String findPage(Model model) {
        model.addAttribute("passwordFindRequest", new PasswordFindRequest());
        return FIND_VIEW;
    }

    /* API-USR-12 */
    @PostMapping("/password/find")
    public String find(@Valid @ModelAttribute PasswordFindRequest passwordFindRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return FIND_VIEW;
        }
        passwordResetService.sendResetMail(passwordFindRequest.getEmail().strip());
        return "redirect:/password/find?sent=true";
    }

    /* API-USR-13 */
    @GetMapping("/password/reset")
    public String resetPage(@RequestParam(required = false) String token, Model model) {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken(token);
        model.addAttribute("passwordResetRequest", request);
        try {
            passwordResetService.checkLink(token);
        } catch (BusinessException e) {
            return expired(e, model);
        }
        return RESET_VIEW;
    }

    /* API-USR-14 */
    @PostMapping("/password/reset")
    public String reset(@Valid @ModelAttribute PasswordResetRequest passwordResetRequest, BindingResult bindingResult,
                        Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return RESET_VIEW;
        }
        try {
            passwordResetService.reset(passwordResetRequest.getToken(), passwordResetRequest.getNewPassword());
        } catch (BusinessException e) {
            return expired(e, model);
        }
        redirectAttributes.addFlashAttribute("toast", "비밀번호가 변경되었습니다");
        return "redirect:/login";
    }

    private static String expired(BusinessException e, Model model) {
        if (e.getErrorCode() != AuthErrorCode.TOKEN_EXPIRED && e.getErrorCode() != AuthErrorCode.TOKEN_USED) {
            throw e;
        }
        model.addAttribute("expired", true);
        return RESET_VIEW;
    }
}
