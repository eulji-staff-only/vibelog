package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.lesson.model.dto.ActivityFormRequest;
import com.vibelog.domain.lesson.model.service.LessonActivityAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/*
 * [LessonActivityAdminController]
 * 역할 : 운영자의 활동 추가·수정·사용 중지·순서 변경 요청을 받는다 (A-06, API-ADM-21~24, FR-LSN-011, #101).
 * 책임 : - 운영자만 들어온다 (SecurityConfig 가 /admin/** 을 막는다).
 *        - 모두 A-06 화면 안 폼이 보내고, 처리 뒤 그 수업 편집 화면으로 돌려보낸다. 입력 오류는 같은 폼을
 *          다시 그릴 자리가 없어(명세에 그런 흐름이 없다) 오류 화면으로 보낸다 (GlobalExceptionHandler).
 */
@Controller
@RequiredArgsConstructor
public class LessonActivityAdminController {

    private final LessonActivityAdminService lessonActivityAdminService;

    /* estimatedMinutes 는 활동에서는 선택 입력(sql/01-schema.sql activity.estimated_minutes NULL 허용)이라
     * 빈 칸으로 보내면 "형식이 올바르지 않음"이 아니라 그대로 null로 받는다 */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Integer.class, "estimatedMinutes", new CustomNumberEditor(Integer.class, true));
    }

    /* API-ADM-21 */
    @PostMapping("/admin/lessons/{lessonId}/activities")
    public String create(@PathVariable Long lessonId, @Valid @ModelAttribute ActivityFormRequest activityForm) {
        lessonActivityAdminService.addActivity(lessonId, activityForm);
        return "redirect:/admin/lessons/" + lessonId;
    }

    /* API-ADM-22 */
    @PostMapping("/admin/activities/{activityId}/edit")
    public String edit(@PathVariable Long activityId, @Valid @ModelAttribute ActivityFormRequest activityForm) {
        Long lessonId = lessonActivityAdminService.editActivity(activityId, activityForm);
        return "redirect:/admin/lessons/" + lessonId;
    }

    /* API-ADM-23 */
    @PostMapping("/admin/activities/{activityId}/deactivate")
    public String deactivate(@PathVariable Long activityId) {
        Long lessonId = lessonActivityAdminService.deactivateActivity(activityId);
        return "redirect:/admin/lessons/" + lessonId;
    }

    /* API-ADM-24. 드래그로 바꾼 순서를 저장 버튼으로 반영한다 */
    @PostMapping("/admin/lessons/{lessonId}/activities/order")
    public String reorder(@PathVariable Long lessonId, @RequestParam List<Long> activityIds) {
        lessonActivityAdminService.reorderActivities(lessonId, activityIds);
        return "redirect:/admin/lessons/" + lessonId;
    }
}
