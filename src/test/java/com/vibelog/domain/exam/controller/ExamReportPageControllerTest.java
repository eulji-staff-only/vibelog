package com.vibelog.domain.exam.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.dto.ExamReportView;
import com.vibelog.domain.exam.model.service.ExamReportService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 오답 분석 리포트 요청(API-EXM-05)이 로그인 회원의 memberId 만 Service 에 넘기고, 리포트를 그리거나
 * 제출한 응시가 없으면 모의고사 화면 + toast 로 보내는지 확인한다 (DB 없이 도는 슬라이스 테스트).
 */
@WebMvcTest(ExamReportPageController.class)
class ExamReportPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExamReportService examReportService;

    private RequestPostProcessor login() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }

    private ExamReportView report(Long nextLessonId) {
        return new ExamReportView(7L, 3L, "범위 정하기", 2, 1, 3,
                List.of(ExamReportView.Concept.of(11L, "개념 A", 1, 1), ExamReportView.Concept.of(12L, "개념 B", 0, 1)),
                List.of(new ExamReportView.Wrong(3, "세 번째 문항", null, "정답 보기", "해설입니다", 12L, "개념 B")),
                nextLessonId);
    }

    @Test
    void 리포트에_점수_약한_개념_틀린_문항과_다음_수업_링크를_그린다() throws Exception {
        given(examReportService.getReport(1L, 7L, 2)).willReturn(Optional.of(report(8L)));

        mockMvc.perform(get("/lessons/7/exam/report").param("attempt", "2").with(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("exam/report"))
                .andExpect(content().string(Matchers.containsString("범위 정하기 · 2회차")))
                .andExpect(content().string(Matchers.containsString("약한 부분")))
                .andExpect(content().string(Matchers.containsString("/courses/3/lessons/7?activity=12")))
                .andExpect(content().string(Matchers.containsString("선택하지 않음")))
                .andExpect(content().string(Matchers.containsString("/courses/3/lessons/8")))
                .andExpect(content().string(Matchers.containsString("/lessons/7/exam/attempts")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("포트폴리오로"))))
                .andExpect(content().string(Matchers.containsString("vl-header")));
    }

    @Test
    void 마지막_수업이면_포트폴리오로_안내한다() throws Exception {
        given(examReportService.getReport(1L, 7L, null)).willReturn(Optional.of(report(null)));

        mockMvc.perform(get("/lessons/7/exam/report").with(login()))
                .andExpect(content().string(Matchers.containsString("포트폴리오 과제가 열렸습니다.")))
                .andExpect(content().string(Matchers.containsString("/courses/3/portfolio")));
    }

    @Test
    void 제출한_응시가_없으면_모의고사_화면으로_보내고_안내한다() throws Exception {
        given(examReportService.getReport(1L, 7L, null)).willReturn(Optional.empty());

        mockMvc.perform(get("/lessons/7/exam/report").with(login()))
                .andExpect(redirectedUrl("/lessons/7/exam"))
                .andExpect(flash().attribute("toast", ExamErrorCode.EXAM_NOT_SUBMITTED.getMessage()));
    }
}
