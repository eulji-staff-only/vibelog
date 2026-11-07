package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.model.dto.CourseCompleteView;
import com.vibelog.domain.lesson.model.service.CourseCompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

/*
 * [CourseCompletePageController]
 * 역할 : 코스 완료 화면(GET, U-18, API-ENR-03)을 연다.
 * 책임 : - 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 수강하지 않았으면 코스 상세(/courses/{courseId})로 돌려보낸다 (API 명세 "실패").
 */
@Controller
@RequiredArgsConstructor
public class CourseCompletePageController {

    private final CourseCompleteService courseCompleteService;

    /* U-18 코스 완료 */
    @GetMapping("/courses/{courseId}/complete")
    public String courseComplete(@PathVariable Long courseId,
                                  @AuthenticationPrincipal LoginMember loginMember,
                                  Model model) {
        Optional<CourseCompleteView> view = courseCompleteService.getCourseComplete(loginMember.getMemberId(), courseId);
        if (view.isEmpty()) {
            return "redirect:/courses/" + courseId;
        }

        model.addAttribute("courseComplete", view.get());
        return "lesson/course-complete";
    }
}
