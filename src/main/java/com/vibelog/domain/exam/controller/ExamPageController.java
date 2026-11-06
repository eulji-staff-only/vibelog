package com.vibelog.domain.exam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [ExamPageController]
 * 역할 : 모의고사 영역의 화면(GET)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #55)
 *        - 응시·이어 풀기·잠김 판단 같은 데이터는 ③ 단계에서 API 명세(API-EXM-01~05)대로 붙인다.
 */
@Controller
public class ExamPageController {

    /* U-15 모의고사 응시 */
    @GetMapping("/lessons/{lessonId}/exam")
    public String exam() {
        return "exam/exam";
    }
}
