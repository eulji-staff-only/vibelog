package com.vibelog.domain.exam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [ExamReportPageController]
 * 역할 : 오답 분석 리포트 화면(U-16)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #56)
 *        - 리포트 데이터·확인 일시 기록(PL-EXM-007)은 ③ 단계에서 API 명세(API-EXM-05)대로 붙인다.
 */
@Controller
public class ExamReportPageController {

    /* U-16 오답 분석 리포트 (?attempt= 는 ③ 단계에서 받는다) */
    @GetMapping("/lessons/{lessonId}/exam/report")
    public String report() {
        return "exam/report";
    }
}
