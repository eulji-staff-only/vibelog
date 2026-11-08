package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dto.ActivityStatus;
import com.vibelog.domain.lesson.model.dto.CompleteActivityResult;
import com.vibelog.domain.lesson.model.dto.LessonActivityCard;
import com.vibelog.domain.lesson.model.dto.LessonPageData;
import com.vibelog.domain.lesson.model.dto.LessonPageResult;
import com.vibelog.domain.lesson.model.service.LessonActivityService;
import com.vibelog.domain.lesson.model.service.LessonPageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.FlashMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * API-LSN-01·02 를 Service 는 모킹하고 Controller 흐름(로그인 회원 꺼내기 · redirect · 모델 전달)만 확인한다
 * (DB 연결 없이 도는 슬라이스 테스트). 데이터 계산 자체는 LessonPageServiceTest·LessonActivityServiceTest(실제 DB) 참고.
 */
@WebMvcTest(LessonPageController.class)
class LessonPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LessonPageService lessonPageService;

    @MockitoBean
    LessonActivityService lessonActivityService;

    private UsernamePasswordAuthenticationToken loginAs(long memberId) {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(memberId);
        return new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER")));
    }

    @Test
    void 열린_수업이면_수업_화면을_그린다() throws Exception {
        LessonActivityCard quiz = new LessonActivityCard(2L, LessonActivityCard.QUIZ, "퀴즈 1",
                ActivityStatus.IN_PROGRESS, true, null, null, null, null, null, null, null, null);
        LessonPageData data = new LessonPageData(1L, 1L, "수업 1", "목표", List.of(quiz));
        given(lessonPageService.getPage(1L, 1L, 1L, null)).willReturn(LessonPageResult.ok(data));

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/lesson"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("활동 목록")));
    }

    @Test
    void 수강_안_한_코스면_코스_상세로_보낸다() throws Exception {
        given(lessonPageService.getPage(1L, 1L, 1L, null)).willReturn(LessonPageResult.notEnrolled());

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1"));
    }

    @Test
    void 잠긴_수업이면_내_학습으로_토스트와_함께_보낸다() throws Exception {
        given(lessonPageService.getPage(1L, 1L, 1L, null)).willReturn(LessonPageResult.locked());

        MvcResult result = mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/learning"))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("toast")).isEqualTo(LessonErrorCode.LESSON_LOCKED.getMessage());
    }

    @Test
    void 개념_완료가_잠겨_있으면_수업_화면으로_토스트와_함께_보낸다() throws Exception {
        given(lessonActivityService.completeConcept(1L, 1L, 1L, 10L)).willReturn(CompleteActivityResult.ofLocked());

        MvcResult result = mockMvc.perform(post("/courses/1/lessons/1/activities/10/complete")
                        .with(csrf())
                        .with(authentication(loginAs(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/lessons/1"))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("toast")).isEqualTo(LessonErrorCode.ACTIVITY_LOCKED.getMessage());
    }

    @Test
    void 개념_완료에_성공하면_바로_뒤_퀴즈_화면으로_보낸다() throws Exception {
        given(lessonActivityService.completeConcept(1L, 1L, 1L, 10L))
                .willReturn(CompleteActivityResult.redirectTo("/courses/1/lessons/1/quizzes/11"));

        mockMvc.perform(post("/courses/1/lessons/1/activities/10/complete")
                        .with(csrf())
                        .with(authentication(loginAs(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/lessons/1/quizzes/11"));
    }
}
