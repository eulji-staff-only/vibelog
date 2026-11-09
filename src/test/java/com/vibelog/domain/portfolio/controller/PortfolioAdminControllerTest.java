package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.PortfolioAgentForm;
import com.vibelog.domain.portfolio.model.dto.PortfolioAssignmentAdminView;
import com.vibelog.domain.portfolio.model.dto.PortfolioAssignmentForm;
import com.vibelog.domain.portfolio.model.service.PortfolioAdminService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * A-07 포트폴리오 과제 편집(API-ADM-34~36) 요청이 맞는 화면·redirect 로 답하고,
 * 입력 오류는 같은 폼에 메시지로 다시 보여주는지 확인한다 (DB 없이 도는 슬라이스 테스트).
 * @WebMvcTest 는 SecurityConfig 를 읽지 않아 운영자로 로그인한 것처럼 둔다.
 */
@WebMvcTest(PortfolioAdminController.class)
@WithMockUser(roles = "ADMIN")
class PortfolioAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PortfolioAdminService portfolioAdminService;

    @BeforeEach
    void setUp() {
        given(portfolioAdminService.getView(5L)).willReturn(new PortfolioAssignmentAdminView(5L, "바이브 코스", true, "/uploads/agent.md"));
        given(portfolioAdminService.getAssignmentForm(5L)).willReturn(PortfolioAssignmentForm.of("주제", "목표",
                List.of(new PortfolioAssignmentForm.Step(11L, "첫 스텝"), new PortfolioAssignmentForm.Step(12L, "둘째 스텝"))));
        given(portfolioAdminService.getAgentForm(5L)).willReturn(new PortfolioAgentForm("에이전트 안내"));
    }

    @Test
    void 과제_탭은_저장된_과제와_스텝을_그린다() throws Exception {
        mockMvc.perform(get("/admin/courses/5/portfolio"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/admin-portfolio"))
                .andExpect(content().string(Matchers.containsString("바이브 코스")))
                .andExpect(content().string(Matchers.containsString("둘째 스텝")))
                .andExpect(content().string(Matchers.containsString("name=\"steps[1].guideStepId\"")));
    }

    @Test
    void 평가_에이전트_탭은_안내와_올린_파일을_그린다() throws Exception {
        mockMvc.perform(get("/admin/courses/5/portfolio").param("tab", "agent"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("에이전트 안내")))
                .andExpect(content().string(Matchers.containsString("/uploads/agent.md")));
    }

    @Test
    void 과제를_아직_안_만들었으면_평가_에이전트_대신_안내를_보여준다() throws Exception {
        given(portfolioAdminService.getView(5L)).willReturn(new PortfolioAssignmentAdminView(5L, "바이브 코스", false, null));

        mockMvc.perform(get("/admin/courses/5/portfolio").param("tab", "agent"))
                .andExpect(content().string(Matchers.containsString("과제·스텝을 먼저 저장하면")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"agentGuide\""))));
    }

    @Test
    void 과제를_저장하면_코스_상세로_간다() throws Exception {
        mockMvc.perform(post("/admin/courses/5/portfolio").with(csrf())
                        .param("topic", "주제").param("goal", "목표")
                        .param("steps[0].guideStepId", "11").param("steps[0].content", "첫 스텝")
                        .param("steps[1].guideStepId", "").param("steps[1].content", "새 스텝"))
                .andExpect(redirectedUrl("/admin/courses/5"));

        verify(portfolioAdminService).saveAssignment(eq(5L), any(PortfolioAssignmentForm.class));
    }

    @Test
    void 주제가_비면_저장하지_않고_같은_폼에_메시지를_보여준다() throws Exception {
        mockMvc.perform(post("/admin/courses/5/portfolio").with(csrf()).param("topic", "").param("goal", "목표"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/admin-portfolio"))
                .andExpect(content().string(Matchers.containsString("과제 주제를 입력해 주세요.")));

        verify(portfolioAdminService, never()).saveAssignment(any(), any());
    }

    @Test
    void 다른_과제의_스텝이면_같은_폼에_메시지를_보여준다() throws Exception {
        willThrow(new BusinessException(PortfolioErrorCode.INVALID_INPUT))
                .given(portfolioAdminService).saveAssignment(eq(5L), any());

        mockMvc.perform(post("/admin/courses/5/portfolio").with(csrf())
                        .param("topic", "주제").param("goal", "목표")
                        .param("steps[0].guideStepId", "999").param("steps[0].content", "가로채기"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("입력 값을 확인해주세요.")));
    }

    @Test
    void 평가_에이전트를_저장하면_에이전트_탭으로_간다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("agentFile", "agent.md", "text/markdown", "# 안내".getBytes());

        mockMvc.perform(multipart("/admin/courses/5/portfolio/agent").file(file).with(csrf()).param("agentGuide", "안내"))
                .andExpect(redirectedUrl("/admin/courses/5/portfolio?tab=agent"));
    }

    @Test
    void 허용하지_않은_설정_파일이면_같은_폼에_규칙을_보여준다() throws Exception {
        willThrow(new BusinessException(CommonErrorCode.INVALID_FILE))
                .given(portfolioAdminService).saveAgent(eq(5L), anyString(), any());
        MockMultipartFile file = new MockMultipartFile("agentFile", "run.sh", "text/plain", "#!/bin/sh".getBytes());

        mockMvc.perform(multipart("/admin/courses/5/portfolio/agent").file(file).with(csrf()).param("agentGuide", "안내"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("설정 파일은 md·json·yaml·txt")));
    }
}
