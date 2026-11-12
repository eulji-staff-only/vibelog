package com.vibelog.domain.course.controller;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dto.DiagnosisPreview;
import com.vibelog.domain.course.model.dto.DiagnosisQuestionForm;
import com.vibelog.domain.course.model.service.DiagnosisAdminService;
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

import java.util.List;
import java.util.Optional;

/*
 * [DiagnosisAdminController]
 * 역할 : 운영자의 진단 설정 화면(A-09)과 폼 (API-ADM-41~45, FR-DGN-004, #187).
 * 책임 : - 운영자만 들어온다 (SecurityConfig /admin/**).
 *        - 문항 추가·수정은 같은 화면의 모달 폼이다. "문항 추가"는 ?new, "수정"은 ?questionId= 로 모달을 연 채로 그린다
 *          (트랙 관리 CourseCatalogAdminController 와 같은 방식). 입력 오류는 모달을 연 채로 칸 메시지(④).
 *          보기 칸 오류는 보기 묶음 아래 메시지 하나(optionsError)로 모은다.
 *        - 사용·중지, 테스트 응답 결과는 "같은 화면 + 토스트"(③). 테스트 응답의 미리 보기는 flash 로 넘긴다 (저장하지 않음).
 */
@Controller
@RequiredArgsConstructor
public class DiagnosisAdminController {

    private static final String VIEW = "course/admin-diagnosis";
    private static final String REDIRECT = "redirect:/admin/diagnosis";
    private static final String SCORE_RANGE = "점수는 1 이상, 2,147,483,647 이하로 입력해 주세요.";

    private final DiagnosisAdminService diagnosisAdminService;

    /* API-ADM-41. ?new 면 빈 추가 모달, ?questionId= 면 그 문항 수정 모달을 연 채로 */
    @GetMapping("/admin/diagnosis")
    public String diagnosis(@RequestParam(required = false) Long questionId, @RequestParam(required = false) String create,
                            Model model) {
        if (questionId != null) {
            model.addAttribute("questionForm", diagnosisAdminService.getForm(questionId));
            return view(questionId, true, null, model);
        }
        model.addAttribute("questionForm", new DiagnosisQuestionForm().withBlankRows());
        return view(null, create != null, null, model);
    }

    /* API-ADM-42 */
    @PostMapping("/admin/diagnosis/questions")
    public String createQuestion(@Valid @ModelAttribute("questionForm") DiagnosisQuestionForm form, BindingResult bindingResult,
                                 Model model, RedirectAttributes redirectAttributes) {
        String optionsError = optionsError(form, bindingResult);
        if (!bindingResult.hasFieldErrors("content") && !bindingResult.hasFieldErrors("sortOrder") && optionsError == null) {
            try {
                diagnosisAdminService.createQuestion(form);
                redirectAttributes.addFlashAttribute("toast", "문항을 추가했습니다");
                return REDIRECT;
            } catch (BusinessException e) {
                optionsError = invalidInput(e);
            }
        }
        form.withBlankRows();
        return view(null, true, optionsError, model);
    }

    /* API-ADM-43 */
    @PostMapping("/admin/diagnosis/questions/{diagnosisQuestionId}/edit")
    public String updateQuestion(@PathVariable Long diagnosisQuestionId,
                                 @Valid @ModelAttribute("questionForm") DiagnosisQuestionForm form, BindingResult bindingResult,
                                 Model model, RedirectAttributes redirectAttributes) {
        String optionsError = optionsError(form, bindingResult);
        if (!bindingResult.hasFieldErrors("content") && !bindingResult.hasFieldErrors("sortOrder") && optionsError == null) {
            try {
                diagnosisAdminService.updateQuestion(diagnosisQuestionId, form);
                redirectAttributes.addFlashAttribute("toast", "문항을 수정했습니다");
                return REDIRECT;
            } catch (BusinessException e) {
                optionsError = invalidInput(e);
            }
        }
        form.withBlankRows();
        return view(diagnosisQuestionId, true, optionsError, model);
    }

    /* API-ADM-44 */
    @PostMapping("/admin/diagnosis/questions/{diagnosisQuestionId}/active")
    public String active(@PathVariable Long diagnosisQuestionId, @RequestParam boolean active,
                         RedirectAttributes redirectAttributes) {
        boolean changed = diagnosisAdminService.changeActive(diagnosisQuestionId, active);
        redirectAttributes.addFlashAttribute("toast", !changed ? "사용 중인 문항이 하나는 있어야 합니다. 다른 문항을 먼저 다시 사용하세요."
                : active ? "문항을 다시 사용합니다" : "문항을 사용 중지했습니다");
        return REDIRECT;
    }

    /* API-ADM-45. 저장하지 않고 미리 보기만 flash 로 넘긴다 */
    @PostMapping("/admin/diagnosis/test")
    public String test(@RequestParam(required = false) List<Long> diagnosisOptionIds, RedirectAttributes redirectAttributes) {
        Optional<DiagnosisPreview> preview = diagnosisAdminService.preview(diagnosisOptionIds);
        if (preview.isEmpty()) {
            redirectAttributes.addFlashAttribute("toast", "사용 중인 문항마다 보기를 하나씩 골라 주세요.");
        } else {
            redirectAttributes.addFlashAttribute("preview", preview.get());
        }
        return REDIRECT + "#dg-test";
    }

    private String view(Long editingId, boolean modalOpen, String optionsError, Model model) {
        model.addAttribute("diagnosis", diagnosisAdminService.getView());
        model.addAttribute("editingId", editingId);
        model.addAttribute("modalOpen", modalOpen);
        model.addAttribute("optionsError", optionsError);
        return VIEW;
    }

    /* 보기 칸 오류 하나. 점수가 숫자가 아니거나 INT 범위를 넘으면 바인딩 단계에서 막힌다 */
    private String optionsError(DiagnosisQuestionForm form, BindingResult bindingResult) {
        boolean bindingFailure = bindingResult.getFieldErrors().stream()
                .anyMatch(error -> error.getField().startsWith("options") && error.isBindingFailure());
        return bindingFailure ? SCORE_RANGE : form.optionError();
    }

    /* 서비스가 막은 입력(없는 트랙 등)은 보기 칸 메시지로. 그 밖의 오류는 그대로 던진다 */
    private String invalidInput(BusinessException e) {
        if (e.getErrorCode() != CourseErrorCode.INVALID_INPUT) {
            throw e;
        }
        return e.getMessage();
    }
}
