package com.vibelog.domain.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [QuizPageController]
 * 역할 : 퀴즈 영역의 학습자 화면(GET)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #54)
 *        - 퀴즈 제목·문항·확인·제출·채점(API-LSN-03~06)과 잠김 판단은 ③ 단계 기능 이슈(#108, #109)에서 붙인다.
 */
@Controller
public class QuizPageController {

    /* U-14 퀴즈 */
    @GetMapping("/courses/{courseId}/lessons/{lessonId}/quizzes/{activityId}")
    public String quiz() {
        return "quiz/quiz";
    }
}
