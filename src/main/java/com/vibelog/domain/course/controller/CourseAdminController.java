package com.vibelog.domain.course.controller;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dto.CourseForm;
import com.vibelog.domain.course.model.service.CourseAdminService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.ErrorCode;
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

import java.util.Map;

/*
 * [CourseAdminController]
 * 역할 : 운영자의 코스 목록(A-04)·코스 만들기·상세(A-05) 화면과 폼 (API-ADM-11~16, FR-CRS-005·006, #96·#117).
 * 책임 : - 운영자만 들어온다 (SecurityConfig /admin/**).
 *        - 입력 오류는 "같은 폼 다시 + 칸별 메시지"(_template.md ④, INVALID_INPUT). 없는 트랙·카테고리를 보낸 경우도
 *          그 칸 메시지로 바꾼다 (트랙·카테고리 관리 CourseCatalogAdminController 와 같은 방식). 나머지 오류는 그대로 던진다.
 *        - 수업 만들기·순서 저장은 이 화면(A-05)의 폼이 lesson 영역 주소(API-ADM-17·18)로 보낸다.
 *        - 공개·비공개(API-ADM-16, #117)는 확인 모달이 보낸다. 공개 조건이 모자라면 상세로 돌아가 토스트로 알린다(③).
 */
@Controller
@RequiredArgsConstructor
public class CourseAdminController {

    private static final String LIST_VIEW = "course/admin-courses";
    private static final String COURSE_VIEW = "course/admin-course";

    /* 폼 칸 메시지로 돌려보낼 코드 → 칸 이름 */
    private static final Map<ErrorCode, String> FIELD_ERRORS = Map.of(
            CourseErrorCode.TRACK_NOT_FOUND, "trackId",
            CourseErrorCode.CATEGORY_NOT_FOUND, "courseCategoryId");

    private final CourseAdminService courseAdminService;

    /* API-ADM-11 */
    @GetMapping("/admin/courses")
    public String courses(@RequestParam(required = false) Long track, @RequestParam(required = false) Long category,
                          Model model) {
        model.addAttribute("courses", courseAdminService.getCourses(track, category));
        model.addAttribute("selectedTrack", track);
        model.addAttribute("selectedCategory", category);
        addOptions(model);
        return LIST_VIEW;
    }

    /* API-ADM-12 */
    @GetMapping("/admin/courses/new")
    public String newCourse(Model model) {
        model.addAttribute("courseForm", new CourseForm());
        return courseView(null, model);
    }

    /* API-ADM-13. 비공개로 저장하고 상세로 */
    @PostMapping("/admin/courses")
    public String createCourse(@Valid @ModelAttribute CourseForm courseForm, BindingResult bindingResult,
                               Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Long courseId = courseAdminService.createCourse(courseForm);
                redirectAttributes.addFlashAttribute("toast", "코스를 만들었습니다. 비공개로 저장했습니다");
                return "redirect:/admin/courses/" + courseId;
            } catch (BusinessException e) {
                toFieldError(e, bindingResult);
            }
        }
        return courseView(null, model);
    }

    /* API-ADM-14 */
    @GetMapping("/admin/courses/{courseId}")
    public String course(@PathVariable Long courseId, Model model) {
        model.addAttribute("courseForm", courseAdminService.getCourseForm(courseId));
        return courseView(courseId, model);
    }

    /* API-ADM-15 */
    @PostMapping("/admin/courses/{courseId}/edit")
    public String updateCourse(@PathVariable Long courseId, @Valid @ModelAttribute CourseForm courseForm,
                               BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                courseAdminService.updateCourse(courseId, courseForm);
                redirectAttributes.addFlashAttribute("toast", "코스를 수정했습니다");
                return "redirect:/admin/courses/" + courseId;
            } catch (BusinessException e) {
                toFieldError(e, bindingResult);
            }
        }
        return courseView(courseId, model);
    }

    /* API-ADM-16 */
    @PostMapping("/admin/courses/{courseId}/visibility")
    public String visibility(@PathVariable Long courseId, @RequestParam boolean isPublic, RedirectAttributes redirectAttributes) {
        boolean changed = courseAdminService.changeVisibility(courseId, isPublic);
        redirectAttributes.addFlashAttribute("toast", !changed ? CourseErrorCode.COURSE_NOT_PUBLISHABLE.getMessage()
                : isPublic ? "코스를 공개했습니다" : "코스를 비공개했습니다");
        return "redirect:/admin/courses/" + courseId;
    }

    /* 만들기(courseId == null)와 상세는 같은 화면. 상세면 수업 목록·과제 상태를 같이 싣는다 */
    private String courseView(Long courseId, Model model) {
        model.addAttribute("course", courseId == null ? null : courseAdminService.getCourse(courseId));
        addOptions(model);
        return COURSE_VIEW;
    }

    private void addOptions(Model model) {
        model.addAttribute("trackOptions", courseAdminService.getTrackOptions());
        model.addAttribute("categoryOptions", courseAdminService.getCategoryOptions());
    }

    /* 칸 메시지로 바꿀 코드가 아니면 그대로 던져 공통 오류 화면으로 보낸다 */
    private void toFieldError(BusinessException e, BindingResult bindingResult) {
        String field = FIELD_ERRORS.get(e.getErrorCode());
        if (field == null) {
            throw e;
        }
        bindingResult.rejectValue(field, e.getErrorCode().name(), e.getMessage());
    }
}
