package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.model.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [LessonEnrollmentController]
 * 역할 : 수강 시작·재시작(API-ENR-01)과 수강 포기(API-ENR-04, P2) 요청을 받는다.
 * 책임 : 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        수강 시작 버튼(폼, th:action)은 코스 상세 이슈(#98)가, 수강 포기 버튼과 확인 모달(M-12)은 내 학습 화면(U-12)이 둔다.
 *        포기했던 코스를 다시 시작하면 토스트 "이어서 학습합니다", 포기하면 "수강을 포기했습니다"를 내 학습에서 보여 준다.
 */
@Controller
@RequiredArgsConstructor
public class LessonEnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enroll")
    public String enroll(@PathVariable Long courseId, @AuthenticationPrincipal LoginMember loginMember,
                         RedirectAttributes redirectAttributes) {
        if (enrollmentService.enroll(loginMember.getMemberId(), courseId)) {
            redirectAttributes.addFlashAttribute("toast", "이어서 학습합니다");
        }
        return "redirect:/my/learning";
    }

    /* API-ENR-04 수강 포기 (M-12 확인 후) */
    @PostMapping("/courses/{courseId}/drop")
    public String drop(@PathVariable Long courseId, @AuthenticationPrincipal LoginMember loginMember,
                       RedirectAttributes redirectAttributes) {
        enrollmentService.drop(loginMember.getMemberId(), courseId);
        redirectAttributes.addFlashAttribute("toast", "수강을 포기했습니다");
        return "redirect:/my/learning";
    }
}
