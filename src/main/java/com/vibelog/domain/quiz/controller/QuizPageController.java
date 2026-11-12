package com.vibelog.domain.quiz.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.quiz.exception.QuizErrorCode;
import com.vibelog.domain.quiz.model.dto.QuizPageView;
import com.vibelog.domain.quiz.model.service.QuizSolveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/*
 * [QuizPageController]
 * 역할 : 퀴즈 화면(GET, U-14, API-LSN-03)을 연다. 문항은 화면이 풀이 시작 API 로 받는다.
 * 책임 : - 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 활동이 닫혀 있으면 에러 화면이 아니라 "수업 화면 + 토스트"(_template.md 0-1 ③, ACTIVITY_LOCKED)로 보낸다.
 *          LessonPageController 와 같은 방식(flash "toast")이다.
 */
@Controller
@RequiredArgsConstructor
public class QuizPageController {

    private final QuizSolveService quizSolveService;

    /* U-14 퀴즈 */
    @GetMapping("/courses/{courseId}/lessons/{lessonId}/quizzes/{activityId}")
    public String quiz(@PathVariable Long courseId, @PathVariable Long lessonId, @PathVariable Long activityId,
                       @AuthenticationPrincipal LoginMember loginMember,
                       Model model, RedirectAttributes redirectAttributes) {
        Optional<QuizPageView> page = quizSolveService.getPage(loginMember.getMemberId(), activityId);
        if (page.isEmpty()) {
            redirectAttributes.addFlashAttribute("toast", QuizErrorCode.ACTIVITY_LOCKED.getMessage());
            return "redirect:/courses/" + courseId + "/lessons/" + lessonId;
        }
        model.addAttribute("quiz", page.get());
        return "quiz/quiz";
    }
}
