package com.vibelog.domain.lesson.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [MyLearningPageController]
 * 역할 : 수강(내 학습 · 코스 완료) 관련 화면(GET)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #47)
 *        - 코스·수업 진행 상태, 완료 조건 같은 데이터는 ③ 단계에서 API 명세대로 붙인다.
 *          tab 쿼리는 화면 안에서 자바스크립트가 읽어 처음 보여줄 탭을 고르는 데만 쓴다.
 */
@Controller
public class MyLearningPageController {

    /* U-12 내 학습 */
    @GetMapping("/my/learning")
    public String myLearning() {
        return "lesson/my-learning";
    }

    /* U-18 코스 완료 */
    @GetMapping("/courses/{courseId}/complete")
    public String courseComplete() {
        return "lesson/course-complete";
    }
}
