package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.model.dto.LessonExamStatus;
import com.vibelog.domain.lesson.model.dto.LessonStatus;
import com.vibelog.domain.lesson.model.dto.MyLearningCourseCard;
import com.vibelog.domain.lesson.model.dto.MyLearningLessonCard;
import com.vibelog.domain.lesson.model.dto.PortfolioStatus;
import com.vibelog.domain.lesson.model.service.MyLearningService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/* 내 학습 화면(U-12, API-ENR-02) 요청이 로그인 회원의 memberId 만 Service 에 넘기고,
   두 탭의 코스 카드를 모두 모델에 담는지 확인한다 (DB 연결 없이 도는 슬라이스 테스트). */
@WebMvcTest(MyLearningPageController.class)
class MyLearningPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyLearningService myLearningService;

    @Test
    void 내_학습_화면을_연다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        given(myLearningService.getActiveCourses(1L)).willReturn(List.of());
        given(myLearningService.getCompletedCourses(1L)).willReturn(List.of());

        mockMvc.perform(get("/my/learning")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/my-learning"))
                .andExpect(model().attribute("activeCourses", List.of()))
                .andExpect(model().attribute("completedCourses", List.of()))
                .andExpect(content().string(Matchers.containsString("내 학습")));
    }

    @Test
    void tab_쿼리가_있어도_같은_화면을_연다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        given(myLearningService.getActiveCourses(1L)).willReturn(List.of());
        given(myLearningService.getCompletedCourses(1L)).willReturn(List.of());

        mockMvc.perform(get("/my/learning").queryParam("tab", "completed")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/my-learning"));
    }

    @Test
    void 코스_카드_데이터가_화면에_그려진다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);

        MyLearningLessonCard lesson = new MyLearningLessonCard(
                1L, 1, "수업 1", LessonStatus.IN_PROGRESS, LessonExamStatus.OPEN);
        MyLearningCourseCard course = new MyLearningCourseCard(
                1L, "테스트 트랙", "테스트 코스", 33, "[수업 1] 모의고사 응시하기",
                List.of(lesson), PortfolioStatus.LOCKED,
                List.of("[수업 1] 모의고사 응시하기", "포트폴리오 — 마지막 수업 리포트를 확인하면 열립니다"),
                false, null);

        given(myLearningService.getActiveCourses(1L)).willReturn(List.of(course));
        given(myLearningService.getCompletedCourses(1L)).willReturn(List.of());

        mockMvc.perform(get("/my/learning")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("테스트 코스")))
                .andExpect(content().string(Matchers.containsString("33%")));
    }

    @Test
    void 리포트_링크가_있는_수업에만_리포트_보기가_그려진다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        MyLearningLessonCard withReport = new MyLearningLessonCard(
                11L, 1, "응시한 수업", LessonStatus.IN_PROGRESS, LessonExamStatus.ATTEMPTED,
                "/lessons/11/exam/report?attempt=2");
        MyLearningLessonCard withoutReport = new MyLearningLessonCard(
                12L, 2, "안 응시한 수업", LessonStatus.IN_PROGRESS, LessonExamStatus.OPEN);
        MyLearningCourseCard course = new MyLearningCourseCard(
                1L, "테스트 트랙", "테스트 코스", 0, "다음", List.of(withReport, withoutReport),
                PortfolioStatus.LOCKED, List.of(), false, null);
        given(myLearningService.getActiveCourses(1L)).willReturn(List.of(course));
        given(myLearningService.getCompletedCourses(1L)).willReturn(List.of());

        mockMvc.perform(get("/my/learning")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.allOf(
                        Matchers.containsString("href=\"/lessons/11/exam/report?attempt=2\""),
                        Matchers.containsString("리포트 보기"),
                        Matchers.not(Matchers.containsString("/lessons/12/exam/report")))));
    }

    @Test
    void 리포트_링크가_없는_수업만_있으면_리포트_보기가_없다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        MyLearningLessonCard lesson = new MyLearningLessonCard(
                12L, 1, "안 응시한 수업", LessonStatus.IN_PROGRESS, LessonExamStatus.OPEN);
        MyLearningCourseCard course = new MyLearningCourseCard(
                1L, "테스트 트랙", "테스트 코스", 0, "다음", List.of(lesson),
                PortfolioStatus.LOCKED, List.of(), false, null);
        given(myLearningService.getActiveCourses(1L)).willReturn(List.of(course));
        given(myLearningService.getCompletedCourses(1L)).willReturn(List.of());

        mockMvc.perform(get("/my/learning")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("리포트 보기"))));
    }

    @Test
    void 수강_중_코스에는_수강_포기_버튼과_확인_모달이_있고_완료_코스에는_없다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        MyLearningCourseCard active = new MyLearningCourseCard(
                7L, "테스트 트랙", "수강 중 코스", 10, "[수업 1] 학습하기",
                List.of(), PortfolioStatus.LOCKED, List.of(), false, null);
        MyLearningCourseCard completed = new MyLearningCourseCard(
                8L, "테스트 트랙", "완료 코스", 100, "완료",
                List.of(), PortfolioStatus.SUBMITTED, List.of(), true, LocalDateTime.of(2026, 11, 1, 0, 0));
        given(myLearningService.getActiveCourses(1L)).willReturn(List.of(active));
        given(myLearningService.getCompletedCourses(1L)).willReturn(List.of(completed));

        mockMvc.perform(get("/my/learning")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("data-ui-modal-open=\"drop-modal-7\"")))
                .andExpect(content().string(Matchers.containsString("id=\"drop-modal-7\"")))
                .andExpect(content().string(Matchers.containsString("action=\"/courses/7/drop\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("drop-modal-8"))));
    }
}
