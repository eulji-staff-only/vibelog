package com.vibelog.domain.course.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [CoursePageController]
 * 역할 : 트랙·코스 영역의 학습자 화면(GET)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #53)
 *        - 트랙·코스·수업 데이터와 필터(track·category·q), 수강 상태는 ③ 단계 기능 이슈(#97, #98)에서 API 명세대로 붙인다.
 */
@Controller
public class CoursePageController {

    /* U-09 트랙 소개 */
    @GetMapping("/tracks/{trackId}")
    public String track() {
        return "course/track";
    }

    /* U-10 코스 목록 (?track=&category=&q= 는 ③ 단계에서 받는다) */
    @GetMapping("/courses")
    public String courses() {
        return "course/list";
    }

    /* U-11 코스 상세 */
    @GetMapping("/courses/{courseId}")
    public String course() {
        return "course/detail";
    }
}
