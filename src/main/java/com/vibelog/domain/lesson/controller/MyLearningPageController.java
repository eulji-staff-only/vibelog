package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.model.service.MyLearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [MyLearningPageController]
 * 역할 : 내 학습 화면(U-12, API-ENR-02) 요청을 받는다.
 * 책임 : 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        tab 쿼리는 화면 안 자바스크립트가 처음 보여줄 탭을 고르는 데만 쓰므로, 두 탭의 코스 카드를 모두 모델에 담는다.
 */
@Controller
@RequiredArgsConstructor
public class MyLearningPageController {

    private final MyLearningService myLearningService;

    @GetMapping("/my/learning")
    public String myLearning(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        Long memberId = loginMember.getMemberId();
        model.addAttribute("activeCourses", myLearningService.getActiveCourses(memberId));
        model.addAttribute("completedCourses", myLearningService.getCompletedCourses(memberId));
        return "lesson/my-learning";
    }
}
