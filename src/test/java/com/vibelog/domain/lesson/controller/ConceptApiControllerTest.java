package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.model.dto.BookmarkResult;
import com.vibelog.domain.lesson.model.dto.ConceptSearchItem;
import com.vibelog.domain.lesson.model.service.ConceptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* 개념 북마크(API-LSN-08)·태그 검색(API-LSN-09) 엔드포인트가 로그인 회원의 memberId 만 Service 에 넘기고
   약속한 JSON 모양으로 답하는지 확인한다 (DB 연결 없이 도는 슬라이스 테스트). */
@WebMvcTest(ConceptApiController.class)
class ConceptApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConceptService conceptService;

    @Test
    void 북마크_토글은_회원_ID로_부르고_bookmarked를_돌려준다() throws Exception {
        given(conceptService.toggleBookmark(1L, 5L)).willReturn(new BookmarkResult(true));

        mockMvc.perform(post("/api/concepts/{activityId}/bookmark", 5L).with(csrf()).with(learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));
    }

    @Test
    void 태그_검색은_약속한_JSON_목록을_돌려준다() throws Exception {
        given(conceptService.searchByTag(3L)).willReturn(List.of(
                new ConceptSearchItem(5L, "개념", 1L, 2L, "코스", "수업")));

        mockMvc.perform(get("/api/concepts").param("conceptTagId", "3").with(learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityId").value(5))
                .andExpect(jsonPath("$[0].title").value("개념"))
                .andExpect(jsonPath("$[0].courseId").value(1))
                .andExpect(jsonPath("$[0].lessonId").value(2))
                .andExpect(jsonPath("$[0].courseTitle").value("코스"))
                .andExpect(jsonPath("$[0].lessonTitle").value("수업"));
    }

    @Test
    void conceptTagId가_없으면_빈_목록을_돌려준다() throws Exception {
        given(conceptService.searchByTag(null)).willReturn(List.of());

        mockMvc.perform(get("/api/concepts").with(learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private RequestPostProcessor learner() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }
}
