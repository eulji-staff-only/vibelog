package com.vibelog.domain.course.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.course.model.service.CourseBrowseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * [CoursePageController]
 * 역할 : 트랙·코스 영역의 학습자 화면(GET)을 연다.
 * 책임 : - 트랙 소개(API-CRS-01)·코스 목록(API-CRS-02)은 CourseBrowseService 가 만든 데이터로 그린다 (#97).
 *        - 비회원도 열린다(SecurityConfig). 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 카테고리·검색(category, q)은 P2라 받지 않는다. 코스 상세 데이터는 #98 에서 붙인다.
 */
@Controller
@RequiredArgsConstructor
public class CoursePageController {

    private final CourseBrowseService courseBrowseService;

    /* U-09 트랙 소개 (API-CRS-01) */
    @GetMapping("/tracks/{trackId}")
    public String track(@PathVariable Long trackId, Model model) {
        model.addAttribute("track", courseBrowseService.getTrackIntro(trackId));
        return "course/track";
    }

    /* U-10 코스 목록 (API-CRS-02) */
    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Long track,
                          @AuthenticationPrincipal LoginMember loginMember,
                          Model model) {
        Long memberId = loginMember == null ? null : loginMember.getMemberId();
        model.addAttribute("courseList", courseBrowseService.getCourseList(memberId, track));
        return "course/list";
    }

    /* U-11 코스 상세 (데이터는 #98 에서 붙인다) */
    @GetMapping("/courses/{courseId}")
    public String course() {
        return "course/detail";
    }
}
