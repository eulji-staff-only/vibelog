package com.vibelog.domain.exam.controller;

import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.dto.ExamQuestionForm;
import com.vibelog.domain.exam.model.service.ExamAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * [ExamAdminController]
 * 역할 : 운영자의 모의고사 문항 추가·수정·사용 여부 변경을 받는다 (A-06 모의고사 탭, API-ADM-29~31, FR-EXM-007).
 * 책임 : - 운영자만 들어온다 (SecurityConfig 가 /admin/** 을 막는다).
 *        - 처리 뒤 A-06 모의고사 탭(/admin/lessons/{lessonId}?tab=exam)으로 돌려보낸다.
 *        - 입력 오류는 같은 폼 다시 + 칸별 메시지(_template.md 7-1 ④)다. A-06 은 lesson 영역 화면이라 여기서 그리지 않고,
 *          입력 값·칸별 메시지·어느 모달인지를 flash 로 넘겨 A-06 이 그 모달을 열린 채로 다시 그린다.
 */
@Controller
@RequiredArgsConstructor
public class ExamAdminController {

    static final String FORM = "examQuestionForm";
    static final String ERRORS = "examQuestionErrors";
    static final String TARGET = "examQuestionTarget";   // "new" 또는 수정 중인 문항 ID

    private final ExamAdminService examAdminService;

    /* API-ADM-29 */
    @PostMapping("/admin/lessons/{lessonId}/exam-questions")
    public String create(@PathVariable Long lessonId, @Valid @ModelAttribute ExamQuestionForm form,
                         BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (!keepErrors(form, bindingResult, "new", redirectAttributes)) {
            examAdminService.create(lessonId, form);
        }
        return examTab(lessonId);
    }

    /* API-ADM-30. 요청에는 문항 ID 만 있어 돌아갈 수업 ID 는 Service 가 문항에서 찾아 준다 */
    @PostMapping("/admin/exam-questions/{examQuestionId}/edit")
    public String edit(@PathVariable Long examQuestionId, @Valid @ModelAttribute ExamQuestionForm form,
                       BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (keepErrors(form, bindingResult, String.valueOf(examQuestionId), redirectAttributes)) {
            return examTab(examAdminService.getLessonId(examQuestionId));
        }
        return examTab(examAdminService.edit(examQuestionId, form));
    }

    /* API-ADM-31 */
    @PostMapping("/admin/exam-questions/{examQuestionId}/active")
    public String active(@PathVariable Long examQuestionId, @RequestParam boolean active) {
        return examTab(examAdminService.changeActive(examQuestionId, active));
    }

    /* 칸별 메시지가 있으면 flash 에 담고 true. 보기 4칸의 오류는 "choices" 한 칸으로 모은다 */
    private boolean keepErrors(ExamQuestionForm form, BindingResult bindingResult, String target,
                               RedirectAttributes redirectAttributes) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getConceptActivityId() == null) {
            errors.put("conceptActivityId", ExamErrorCode.CONCEPT_REQUIRED.getMessage());
        }
        for (FieldError error : bindingResult.getFieldErrors()) {
            String field = error.getField().startsWith("choices") ? "choices" : error.getField();
            errors.putIfAbsent(field, error.isBindingFailure() ? "입력 값을 확인해주세요." : error.getDefaultMessage());
        }
        if (errors.isEmpty()) {
            return false;
        }
        redirectAttributes.addFlashAttribute(FORM, form);
        redirectAttributes.addFlashAttribute(ERRORS, errors);
        redirectAttributes.addFlashAttribute(TARGET, target);
        return true;
    }

    private String examTab(Long lessonId) {
        return "redirect:/admin/lessons/" + lessonId + "?tab=exam";
    }
}
