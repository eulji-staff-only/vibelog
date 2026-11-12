package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dto.CompleteActivityResult;
import com.vibelog.domain.lesson.model.dto.LessonPageResult;
import com.vibelog.domain.lesson.model.service.LessonActivityService;
import com.vibelog.domain.lesson.model.service.LessonPageService;
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
 * [LessonPageController]
 * 역할 : 수업 화면 조회(API-LSN-01)와 개념 읽기 완료(API-LSN-02)를 받는다.
 * 책임 : - 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 운영자(ROLE_ADMIN)면 미리보기(FR-LSN-015, API-ADM-39)로 Service 에 알린다. 역할은 여기서만 꺼낸다.
 *        - LESSON_LOCKED·ACTIVITY_LOCKED 는 에러 화면이 아니라 "다른 화면 + 토스트"(_template.md 0-1 ③)라서
 *          Service 가 돌려준 결과를 보고 여기서 직접 redirect 한다 (BusinessException 으로 던지지 않는다).
 */
@Controller
@RequiredArgsConstructor
public class LessonPageController {

    private final LessonPageService lessonPageService;
    private final LessonActivityService lessonActivityService;

    /* U-13 수업 (활동 목록 · 개념) */
    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    public String lesson(@PathVariable Long courseId, @PathVariable Long lessonId,
                          @RequestParam(required = false) String activity,
                          @AuthenticationPrincipal LoginMember loginMember,
                          Model model, RedirectAttributes redirectAttributes) {
        LessonPageResult result = lessonPageService
                .getPage(loginMember.getMemberId(), courseId, lessonId, activity, loginMember.isAdmin());
        return switch (result.status()) {
            case NOT_ENROLLED -> "redirect:/courses/" + courseId;
            case LOCKED -> {
                redirectAttributes.addFlashAttribute("toast", LessonErrorCode.LESSON_LOCKED.getMessage());
                yield "redirect:/my/learning";
            }
            case OK -> {
                model.addAttribute("page", result.data());
                yield "lesson/lesson";
            }
        };
    }

    /* API-LSN-02 개념 읽기 완료 */
    @PostMapping("/courses/{courseId}/lessons/{lessonId}/activities/{activityId}/complete")
    public String completeActivity(@PathVariable Long courseId, @PathVariable Long lessonId,
                                    @PathVariable Long activityId,
                                    @AuthenticationPrincipal LoginMember loginMember,
                                    RedirectAttributes redirectAttributes) {
        CompleteActivityResult result = lessonActivityService
                .completeConcept(loginMember.getMemberId(), courseId, lessonId, activityId);
        if (result.locked()) {
            redirectAttributes.addFlashAttribute("toast", LessonErrorCode.ACTIVITY_LOCKED.getMessage());
            return "redirect:/courses/" + courseId + "/lessons/" + lessonId;
        }
        return "redirect:" + result.redirectUrl();
    }
}
