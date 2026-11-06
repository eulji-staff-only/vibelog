package com.vibelog.domain.lesson.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [CourseCompletePageController]
 * 역할 : 코스 완료 화면(GET)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #47)
 *        - 완료 조건 같은 데이터는 ③ 단계(#115)에서 API 명세대로 붙인다.
 */
@Controller
public class CourseCompletePageController {

    /* U-18 코스 완료 */
    @GetMapping("/courses/{courseId}/complete")
    public String courseComplete() {
        return "lesson/course-complete";
    }
}
