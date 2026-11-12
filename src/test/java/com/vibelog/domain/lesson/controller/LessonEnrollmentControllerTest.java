package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.service.EnrollmentService;
import com.vibelog.global.exception.BusinessException;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* 수강 시작(API-ENR-01) 엔드포인트가 로그인 회원의 memberId 만 Service 에 넘기고, 성공하면 내 학습으로 보내는지
   확인한다 (DB 연결 없이 도는 슬라이스 테스트). */
@WebMvcTest(LessonEnrollmentController.class)
class LessonEnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void 수강_시작에_성공하면_내_학습으로_리다이렉트한다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);

        mockMvc.perform(post("/courses/{courseId}/enroll", 1L)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/learning"));
    }

    @Test
    void 처음_시작하면_토스트_없이_내_학습으로_보낸다() throws Exception {
        given(enrollmentService.enroll(1L, 1L)).willReturn(false);

        mockMvc.perform(post("/courses/{courseId}/enroll", 1L).with(csrf()).with(learner()))
                .andExpect(redirectedUrl("/my/learning"))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    void 포기한_코스를_다시_시작하면_이어서_학습한다는_토스트를_보여_준다() throws Exception {
        given(enrollmentService.enroll(1L, 1L)).willReturn(true);

        mockMvc.perform(post("/courses/{courseId}/enroll", 1L).with(csrf()).with(learner()))
                .andExpect(redirectedUrl("/my/learning"))
                .andExpect(flash().attribute("toast", "이어서 학습합니다"));
    }

    @Test
    void 수강을_포기하면_내_학습으로_토스트와_함께_보낸다() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/drop", 1L).with(csrf()).with(learner()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/learning"))
                .andExpect(flash().attribute("toast", "수강을 포기했습니다"));

        verify(enrollmentService).drop(1L, 1L);
    }

    @Test
    void 수강_중이_아니면_포기할_때_ENROLLMENT_NOT_FOUND_오류_화면이다() throws Exception {
        willThrow(new BusinessException(LessonErrorCode.ENROLLMENT_NOT_FOUND))
                .given(enrollmentService).drop(1L, 999L);

        mockMvc.perform(post("/courses/{courseId}/drop", 999L).with(csrf()).with(learner()))
                .andExpect(status().isNotFound());
    }

    private RequestPostProcessor learner() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }

    @Test
    void 코스가_없으면_COURSE_NOT_FOUND_오류_화면이다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        willThrow(new BusinessException(LessonErrorCode.COURSE_NOT_FOUND))
                .given(enrollmentService).enroll(1L, 999L);

        mockMvc.perform(post("/courses/{courseId}/enroll", 999L)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isNotFound());
    }
}
