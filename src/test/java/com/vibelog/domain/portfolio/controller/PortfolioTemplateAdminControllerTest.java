package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.PortfolioTemplateForm;
import com.vibelog.domain.portfolio.model.dto.TemplateItemView;
import com.vibelog.domain.portfolio.model.service.PortfolioTemplateService;
import com.vibelog.global.exception.BusinessException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * A-12 템플릿 관리 요청(API-PRT-14·15)이 화면을 그리고, 저장·입력 오류를 _template.md 7-1 ④대로 처리하는지 확인한다.
 * @WebMvcTest 는 SecurityConfig 를 읽지 않아 운영자로 로그인한 것처럼 둔다.
 */
@WebMvcTest(PortfolioTemplateAdminController.class)
@WithMockUser(roles = "ADMIN")
class PortfolioTemplateAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PortfolioTemplateService portfolioTemplateService;

    @BeforeEach
    void setUp() {
        given(portfolioTemplateService.getItems()).willReturn(TemplateItemView.defaults());
    }

    private MockHttpServletRequestBuilder saveRequest(String firstLabel) {
        MockHttpServletRequestBuilder request = post("/admin/templates").with(csrf());
        String[] keys = {"TITLE", "LINK", "DESCRIPTION", "AI_TOOLS", "RETROSPECTIVE"};
        for (int i = 0; i < keys.length; i++) {
            request.param("items[" + i + "].itemKey", keys[i])
                    .param("items[" + i + "].label", i == 0 ? firstLabel : "이름" + i)
                    .param("items[" + i + "].guide", "");
        }
        return request;
    }

    @Test
    void 템플릿_관리_화면은_항목_5개를_이름_안내_문구와_함께_그린다() throws Exception {
        mockMvc.perform(get("/admin/templates"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/admin-templates"))
                .andExpect(content().string(Matchers.containsString("기본 항목: 사용한 AI 도구")))
                .andExpect(content().string(Matchers.containsString("name=\"items[4].label\"")))
                .andExpect(content().string(Matchers.containsString("value=\"이 작품을 한 줄로 소개해 주세요\"")))
                .andExpect(content().string(Matchers.containsString("href=\"/admin/templates\"")));
    }

    @Test
    void 저장하면_템플릿_관리_화면으로_보내고_안내한다() throws Exception {
        mockMvc.perform(saveRequest("작품 이름"))
                .andExpect(redirectedUrl("/admin/templates"))
                .andExpect(flash().attribute("toast", "템플릿을 저장했습니다"));

        verify(portfolioTemplateService).save(any(PortfolioTemplateForm.class));
    }

    @Test
    void 항목_이름이_비면_저장하지_않고_같은_폼에_메시지를_보여준다() throws Exception {
        mockMvc.perform(saveRequest(""))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/admin-templates"))
                .andExpect(content().string(Matchers.containsString("항목 이름을 입력해 주세요.")));

        verify(portfolioTemplateService, never()).save(any());
    }

    @Test
    void 항목_키가_맞지_않으면_같은_폼에_메시지를_보여준다() throws Exception {
        willThrow(new BusinessException(PortfolioErrorCode.INVALID_INPUT)).given(portfolioTemplateService).save(any());

        mockMvc.perform(saveRequest("작품 이름"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("입력 값을 확인해주세요.")));
    }
}
