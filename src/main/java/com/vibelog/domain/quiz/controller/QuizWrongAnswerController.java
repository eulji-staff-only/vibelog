package com.vibelog.domain.quiz.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.quiz.model.service.QuizWrongAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * [QuizWrongAnswerController]
 * 역할 : 틀린 문제 모아보기 화면 (U-28, API-LSN-10, FR-LSN-008, #190).
 * 책임 : - 회원만 들어온다 (SecurityConfig). 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다.
 *        - course·lesson 은 선택, page 는 생략하면 1 (quiz.md API-LSN-10). 수업 화면(U-13)의 퀴즈 칸에서 그 수업으로 걸러 들어온다.
 */
@Controller
@RequiredArgsConstructor
public class QuizWrongAnswerController {

    private final QuizWrongAnswerService quizWrongAnswerService;

    /* API-LSN-10 */
    @GetMapping("/my/wrong-answers")
    public String wrongAnswers(@RequestParam(required = false) Long course, @RequestParam(required = false) Long lesson,
                               @RequestParam(defaultValue = "1") int page,
                               @AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("wrong", quizWrongAnswerService.getPage(loginMember.getMemberId(), course, lesson, page));
        return "quiz/wrong-answers";
    }
}
