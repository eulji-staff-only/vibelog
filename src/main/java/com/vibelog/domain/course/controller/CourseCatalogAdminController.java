package com.vibelog.domain.course.controller;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dto.CategoryForm;
import com.vibelog.domain.course.model.dto.TrackForm;
import com.vibelog.domain.course.model.service.CourseCatalogAdminService;
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
 * [CourseCatalogAdminController]
 * 역할 : 운영자의 트랙 관리(A-02)·카테고리 관리(A-03) 화면과 폼 (API-ADM-02~09, #95).
 * 책임 : - 운영자만 들어온다 (SecurityConfig /admin/**). 만들기·수정은 같은 화면의 모달 폼이다.
 *        - 입력 오류·이름 중복·순서 중복은 "같은 폼 다시 + 칸별 메시지"(_template.md ④): 모달을 연 채로 다시 그린다.
 *          (공지 관리 MessageAdminNoticeController 와 같은 방식: 그 코드만 칸 메시지로 바꾸고 나머지는 그대로 던진다)
 *        - 코스가 있어 못 지우면 "다른 화면 + 토스트"(③): 목록으로 돌아가 안내한다.
 */
@Controller
@RequiredArgsConstructor
public class CourseCatalogAdminController {

    private static final String TRACKS_VIEW = "course/admin-tracks";
    private static final String CATEGORIES_VIEW = "course/admin-categories";

    /* 폼 칸 메시지로 돌려보낼 코드 → 칸 이름 */
    private static final Map<ErrorCode, String> FIELD_ERRORS = Map.of(
            CourseErrorCode.DUPLICATE_NAME, "name",
            CourseErrorCode.SORT_ORDER_TAKEN, "sortOrder");

    private final CourseCatalogAdminService courseCatalogAdminService;

    /* ---- A-02 트랙 ---- */

    /* API-ADM-02. ?trackId= 가 있으면 그 트랙 수정 모달을 연 채로 */
    @GetMapping("/admin/tracks")
    public String tracks(@RequestParam(required = false) Long trackId, Model model) {
        boolean editing = trackId != null;
        model.addAttribute("trackForm", editing ? courseCatalogAdminService.getTrackForm(trackId) : new TrackForm());
        return tracksView(trackId, editing, model);
    }

    /* API-ADM-03 */
    @PostMapping("/admin/tracks")
    public String createTrack(@Valid @ModelAttribute TrackForm trackForm, BindingResult bindingResult,
                              Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                courseCatalogAdminService.createTrack(trackForm);
                redirectAttributes.addFlashAttribute("toast", "트랙을 만들었습니다");
                return "redirect:/admin/tracks";
            } catch (BusinessException e) {
                toFieldError(e, bindingResult);
            }
        }
        return tracksView(null, true, model);
    }

    /* API-ADM-04 */
    @PostMapping("/admin/tracks/{trackId}/edit")
    public String updateTrack(@PathVariable Long trackId, @Valid @ModelAttribute TrackForm trackForm, BindingResult bindingResult,
                              Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                courseCatalogAdminService.updateTrack(trackId, trackForm);
                redirectAttributes.addFlashAttribute("toast", "트랙을 수정했습니다");
                return "redirect:/admin/tracks";
            } catch (BusinessException e) {
                toFieldError(e, bindingResult);
            }
        }
        return tracksView(trackId, true, model);
    }

    /* API-ADM-05. 코스가 있으면 지우지 않고 목록에서 안내 (③) */
    @PostMapping("/admin/tracks/{trackId}/delete")
    public String deleteTrack(@PathVariable Long trackId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("toast", deleteResult(() -> courseCatalogAdminService.deleteTrack(trackId),
                CourseErrorCode.TRACK_HAS_COURSES, "트랙을 삭제했습니다"));
        return "redirect:/admin/tracks";
    }

    /* ---- A-03 카테고리 ---- */

    /* API-ADM-06. ?courseCategoryId= 가 있으면 그 카테고리 수정 모달을 연 채로 */
    @GetMapping("/admin/categories")
    public String categories(@RequestParam(required = false) Long courseCategoryId, Model model) {
        boolean editing = courseCategoryId != null;
        model.addAttribute("categoryForm", editing ? courseCatalogAdminService.getCategoryForm(courseCategoryId) : new CategoryForm());
        return categoriesView(courseCategoryId, editing, model);
    }

    /* API-ADM-07 */
    @PostMapping("/admin/categories")
    public String createCategory(@Valid @ModelAttribute CategoryForm categoryForm, BindingResult bindingResult,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                courseCatalogAdminService.createCategory(categoryForm);
                redirectAttributes.addFlashAttribute("toast", "카테고리를 만들었습니다");
                return "redirect:/admin/categories";
            } catch (BusinessException e) {
                toFieldError(e, bindingResult);
            }
        }
        return categoriesView(null, true, model);
    }

    /* API-ADM-08 */
    @PostMapping("/admin/categories/{courseCategoryId}/edit")
    public String updateCategory(@PathVariable Long courseCategoryId, @Valid @ModelAttribute CategoryForm categoryForm,
                                 BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                courseCatalogAdminService.updateCategory(courseCategoryId, categoryForm);
                redirectAttributes.addFlashAttribute("toast", "카테고리를 수정했습니다");
                return "redirect:/admin/categories";
            } catch (BusinessException e) {
                toFieldError(e, bindingResult);
            }
        }
        return categoriesView(courseCategoryId, true, model);
    }

    /* API-ADM-09 */
    @PostMapping("/admin/categories/{courseCategoryId}/delete")
    public String deleteCategory(@PathVariable Long courseCategoryId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("toast", deleteResult(() -> courseCatalogAdminService.deleteCategory(courseCategoryId),
                CourseErrorCode.CATEGORY_HAS_COURSES, "카테고리를 삭제했습니다"));
        return "redirect:/admin/categories";
    }

    /* ---- 공통 ---- */

    private String tracksView(Long editingId, boolean modalOpen, Model model) {
        model.addAttribute("tracks", courseCatalogAdminService.getTracks());
        model.addAttribute("editingId", editingId);
        model.addAttribute("modalOpen", modalOpen);
        return TRACKS_VIEW;
    }

    private String categoriesView(Long editingId, boolean modalOpen, Model model) {
        model.addAttribute("categories", courseCatalogAdminService.getCategories());
        model.addAttribute("editingId", editingId);
        model.addAttribute("modalOpen", modalOpen);
        return CATEGORIES_VIEW;
    }

    /* 이름·순서 중복만 그 칸의 메시지로 바꾼다. 나머지(없는 트랙 등)는 그대로 던져 오류 화면으로 */
    private void toFieldError(BusinessException e, BindingResult bindingResult) {
        String field = FIELD_ERRORS.get(e.getErrorCode());
        if (field == null) {
            throw e;
        }
        bindingResult.rejectValue(field, e.getErrorCode().name(), e.getMessage());
    }

    /* 삭제: 코스가 있어 막힌 경우만 안내 문구로 바꾸고, 나머지는 그대로 던진다 */
    private String deleteResult(Runnable delete, ErrorCode blocked, String done) {
        try {
            delete.run();
            return done;
        } catch (BusinessException e) {
            if (e.getErrorCode() != blocked) {
                throw e;
            }
            return e.getMessage();
        }
    }
}
