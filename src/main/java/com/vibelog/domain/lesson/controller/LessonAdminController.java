package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.lesson.model.dto.LessonFormRequest;
import com.vibelog.domain.lesson.model.service.LessonAdminService;
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

import java.util.List;

/*
 * [LessonAdminController]
 * 역할 : 운영자의 수업 만들기·순서·편집(A-06) 요청을 받는다 (API-ADM-17~20, FR-LSN-011, #100).
 * 책임 : - 운영자만 들어온다 (SecurityConfig 가 /admin/** 을 막는다).
 *        - 수업 만들기(API-ADM-17)는 코스 상세(A-05, #96)의 인라인 폼이 보낸다. A-05 화면 파일은 #96 담당이라
 *          여기서는 만들지 않고, 입력 오류는 같은 폼이 없어 같은 폼 재표시 대신 오류 화면으로 보낸다.
 *        - 수업 정보 수정(API-ADM-20)은 이 화면(A-06) 안의 폼이라 틀리면 같은 화면에 칸별 메시지로 다시 보여준다.
 */
@Controller
@RequiredArgsConstructor
public class LessonAdminController {

    private static final String EDIT_VIEW = "lesson/admin-lesson";

    private final LessonAdminService lessonAdminService;

    /* API-ADM-17 */
    @PostMapping("/admin/courses/{courseId}/lessons")
    public String create(@PathVariable Long courseId, @Valid @ModelAttribute LessonFormRequest lessonForm) {
        Long lessonId = lessonAdminService.create(courseId, lessonForm);
        return "redirect:/admin/lessons/" + lessonId;
    }

    /* API-ADM-18. 드래그로 바꾼 순서를 저장 버튼으로 반영한다 (A-05, #96 화면의 폼이 보낸다) */
    @PostMapping("/admin/courses/{courseId}/lessons/order")
    public String reorder(@PathVariable Long courseId, @RequestParam List<Long> lessonIds) {
        lessonAdminService.reorder(courseId, lessonIds);
        return "redirect:/admin/courses/" + courseId;
    }

    /* API-ADM-19 */
    @GetMapping("/admin/lessons/{lessonId}")
    public String edit(@PathVariable Long lessonId, @RequestParam(required = false) String tab, Model model) {
        model.addAttribute("lesson", lessonAdminService.getEditView(lessonId, tab));
        model.addAttribute("lessonForm", lessonAdminService.getForm(lessonId));
        return EDIT_VIEW;
    }

    /* API-ADM-20 */
    @PostMapping("/admin/lessons/{lessonId}/edit")
    public String update(@PathVariable Long lessonId, @Valid @ModelAttribute("lessonForm") LessonFormRequest lessonForm,
                          BindingResult bindingResult, @RequestParam(required = false) String tab, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("lesson", lessonAdminService.getEditView(lessonId, tab));
            return EDIT_VIEW;
        }
        lessonAdminService.update(lessonId, lessonForm);
        return "redirect:/admin/lessons/" + lessonId;
    }
}
