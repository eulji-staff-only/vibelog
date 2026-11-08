package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.model.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/*
 * [LessonEnrollmentController]
 * 역할 : 수강 시작 요청(API-ENR-01)을 받는다.
 * 책임 : 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        버튼(폼, th:action)은 코스 상세 이슈(#98)가 둔다 — 여기서는 엔드포인트만 만든다.
 */
@Controller
@RequiredArgsConstructor
public class LessonEnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enroll")
    public String enroll(@PathVariable Long courseId, @AuthenticationPrincipal LoginMember loginMember) {
        enrollmentService.enroll(loginMember.getMemberId(), courseId);
        return "redirect:/my/learning";
    }
}
