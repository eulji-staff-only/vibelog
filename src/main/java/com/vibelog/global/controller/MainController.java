package com.vibelog.global.controller;

import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.contest.model.service.ContestReader;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.message.model.service.MessageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [MainController]
 * 역할 : 메인(랜딩) 화면 `/` (U-01, API-USR-01) 을 보여준다. 어느 도메인 영역에도 속하지 않는 화면이라 global 에 둔다 (#13).
 * 책임 : - 코스 탭의 대표 코스는 CourseReader 로 공개 코스 앞에서 3개를 받는다 (#409). 카드를 누르면 코스 상세(U-11)로 간다.
 *        - 공통 헤더(1-5)의 닉네임은 HeaderAdvice 가 모든 화면에 넣는다 (#362).
 *        - P2 (#203): 최근 공지(MessageReader)와 이번 주 추천 작품 상위 3개(CommunityReader)를 각 영역 Reader 로 받는다.
 *        - 이달의 콘테스트(주제·출품 마감)는 ContestReader.findCurrent 로 받는다. 없으면 null (명세 API-USR-01).
 */
@Controller
@RequiredArgsConstructor
public class MainController {

    /* 명세는 "최근 공지 N개"라 정해지지 않아 3개로 둔다 (#203 PR 에서 확인) */
    static final int RECENT_NOTICES = 3;
    static final int WEEKLY_TOP = 3;
    /* 대표 코스 개수도 명세에 없어 공지·추천 작품과 같은 3개로 둔다 (#409) */
    static final int FEATURED_COURSES = 3;

    private final MessageReader messageReader;
    private final CommunityReader communityReader;
    private final ContestReader contestReader;
    private final CourseReader courseReader;

    @GetMapping("/")
    public String main(Model model) {
        model.addAttribute("featuredCourses", courseReader.findFeaturedCourses(FEATURED_COURSES));
        model.addAttribute("recentNotices", messageReader.findRecentNotices(RECENT_NOTICES));
        model.addAttribute("weeklyTop", communityReader.findWeeklyTop(WEEKLY_TOP));
        model.addAttribute("currentContest", contestReader.findCurrent().orElse(null));
        return "common/main";
    }
}
