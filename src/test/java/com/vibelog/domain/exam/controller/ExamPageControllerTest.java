package com.vibelog.domain.exam.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.dto.ExamPageView;
import com.vibelog.domain.exam.model.dto.ExamStartResult;
import com.vibelog.domain.exam.model.dto.ExamSubmitResult;
import com.vibelog.domain.exam.model.service.ExamService;
import com.vibelog.global.exception.BusinessException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 모의고사 요청(API-EXM-01~04)이 로그인 회원의 memberId 만 Service 에 넘기고,
 * 결과에 맞는 화면·redirect·토스트·JSON 으로 답하는지 확인한다 (DB 없이 도는 슬라이스 테스트).
 * 템플릿이 실제 데이터로 오류 없이 그려지는지도 여기서 잡는다.
 */
@WebMvcTest({ExamPageController.class, ExamAnswerApiController.class})
class ExamPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExamService examService;

    private RequestPostProcessor login() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }

    @Test
    void 진행_중인_응시가_있으면_문항과_저장된_답을_그린다() throws Exception {
        ExamPageView page = new ExamPageView(7L, 3L, "프롬프트 기초", 11L, 2, List.of(
                new ExamPageView.Question(21L, "첫 문항", List.of(
                        new ExamPageView.Choice(31L, "보기 가"), new ExamPageView.Choice(32L, "보기 나")), 32L),
                new ExamPageView.Question(22L, "둘째 문항", List.of(new ExamPageView.Choice(33L, "보기 다")), null)));
        given(examService.getPage(1L, 7L)).willReturn(Optional.of(page));

        mockMvc.perform(get("/lessons/7/exam").with(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("exam/exam"))
                .andExpect(content().string(Matchers.containsString("프롬프트 기초 · 2회차")))
                .andExpect(content().string(Matchers.containsString("둘째 문항")))
                .andExpect(content().string(Matchers.containsString("aria-pressed=\"true\" data-choice-id=\"32\"")))
                .andExpect(content().string(Matchers.containsString("/api/exam-attempts/11/answers")))
                .andExpect(content().string(Matchers.containsString("/lessons/7/exam/submit")))
                .andExpect(content().string(Matchers.containsString("모의고사를 제출할까요?")));
    }

    @Test
    void 진행_중인_응시가_없으면_수업_화면의_모의고사_항목으로_보낸다() throws Exception {
        given(examService.getPage(1L, 7L)).willReturn(Optional.empty());
        given(examService.getCourseId(7L)).willReturn(3L);

        mockMvc.perform(get("/lessons/7/exam").with(login()))
                .andExpect(redirectedUrl("/courses/3/lessons/7?activity=exam"));
    }

    @Test
    void 응시를_시작하면_모의고사_화면으로_간다() throws Exception {
        given(examService.start(1L, 7L)).willReturn(ExamStartResult.ok());

        mockMvc.perform(post("/lessons/7/exam/attempts").with(login()).with(csrf()))
                .andExpect(redirectedUrl("/lessons/7/exam"));
    }

    @Test
    void 잠긴_모의고사는_내_학습으로_보내고_안내한다() throws Exception {
        given(examService.start(1L, 7L)).willReturn(ExamStartResult.locked());

        mockMvc.perform(post("/lessons/7/exam/attempts").with(login()).with(csrf()))
                .andExpect(redirectedUrl("/my/learning"))
                .andExpect(flash().attribute("toast", ExamErrorCode.EXAM_LOCKED.getMessage()));
    }

    @Test
    void 지난_리포트를_안_봤으면_그_리포트로_보낸다() throws Exception {
        given(examService.start(1L, 7L)).willReturn(ExamStartResult.reportNotViewed(1));

        mockMvc.perform(post("/lessons/7/exam/attempts").with(login()).with(csrf()))
                .andExpect(redirectedUrl("/lessons/7/exam/report?attempt=1"))
                .andExpect(flash().attribute("toast", ExamErrorCode.REPORT_NOT_VIEWED.getMessage()));
    }

    @Test
    void 제출하면_마지막_답을_넘기고_리포트로_간다() throws Exception {
        given(examService.submit(1L, 7L, 11L, Map.of(21L, 32L))).willReturn(new ExamSubmitResult(2, false));

        mockMvc.perform(post("/lessons/7/exam/submit").with(login()).with(csrf())
                        .param("examAttemptId", "11")
                        .param("answers[21]", "32"))
                .andExpect(redirectedUrl("/lessons/7/exam/report?attempt=2"))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    void 이미_제출했으면_리포트로_보내고_안내한다() throws Exception {
        given(examService.submit(1L, 7L, 11L, Map.of())).willReturn(new ExamSubmitResult(2, true));

        mockMvc.perform(post("/lessons/7/exam/submit").with(login()).with(csrf()).param("examAttemptId", "11"))
                .andExpect(redirectedUrl("/lessons/7/exam/report?attempt=2"))
                .andExpect(flash().attribute("toast", ExamErrorCode.ATTEMPT_SUBMITTED.getMessage()));
    }

    @Test
    void 답을_저장하면_204() throws Exception {
        mockMvc.perform(post("/api/exam-attempts/11/answers").with(login()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examQuestionId\": 21, \"examChoiceId\": null}"))
                .andExpect(status().isNoContent());

        verify(examService).saveAnswer(1L, 11L, 21L, null);
    }

    @Test
    void 제출한_응시에_답을_저장하면_JSON_실패_응답() throws Exception {
        willThrow(new BusinessException(ExamErrorCode.ATTEMPT_SUBMITTED))
                .given(examService).saveAnswer(1L, 11L, 21L, 31L);

        mockMvc.perform(post("/api/exam-attempts/11/answers").with(login()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examQuestionId\": 21, \"examChoiceId\": 31}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_SUBMITTED"));
    }
}
