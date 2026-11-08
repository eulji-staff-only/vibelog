package com.vibelog.domain.lesson.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [LessonPageController]
 * 역할 : 수업 영역의 화면(GET)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #48)
 *        - 활동 목록·잠김 여부·개념 본문 같은 데이터는 ③ 단계에서 API 명세대로 붙인다.
 *          activity 쿼리는 화면 안에서 자바스크립트가 읽어 처음 보여줄 활동을 고르는 데만 쓴다.
 */
@Controller
public class LessonPageController {

    /* U-13 수업 (활동 목록 · 개념) */
    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    public String lesson() {
        return "lesson/lesson";
    }
}
