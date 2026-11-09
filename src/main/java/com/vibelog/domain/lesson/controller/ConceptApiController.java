package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.model.dto.BookmarkResult;
import com.vibelog.domain.lesson.model.dto.ConceptSearchItem;
import com.vibelog.domain.lesson.model.service.ConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * [ConceptApiController]
 * 역할 : 수업 화면(U-13)의 북마크 버튼(API-LSN-08)과 개념 태그 검색 모달(API-LSN-09)이 부르는 JSON 을 돌려준다.
 * 책임 : 회원만 (SecurityConfig). 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다.
 */
@RestController
@RequiredArgsConstructor
public class ConceptApiController {

    private final ConceptService conceptService;

    /* API-LSN-08 개념 북마크 토글 */
    @PostMapping("/api/concepts/{activityId}/bookmark")
    public BookmarkResult toggleBookmark(@PathVariable Long activityId, @AuthenticationPrincipal LoginMember loginMember) {
        return conceptService.toggleBookmark(loginMember.getMemberId(), activityId);
    }

    /* API-LSN-09 개념 태그 검색. conceptTagId 가 없으면 빈 목록 */
    @GetMapping("/api/concepts")
    public List<ConceptSearchItem> search(@RequestParam(required = false) Long conceptTagId) {
        return conceptService.searchByTag(conceptTagId);
    }
}
