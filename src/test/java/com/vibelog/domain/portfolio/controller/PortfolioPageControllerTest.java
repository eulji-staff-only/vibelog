package com.vibelog.domain.portfolio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/* 화면 URL 로 들어오면 템플릿이 오류 없이 그려지는지 확인한다. (템플릿 문법 오류를 build 에서 잡는다) */
@WebMvcTest(PortfolioPageController.class)
class PortfolioPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 포트폴리오_과제_화면을_연다() throws Exception {
        mockMvc.perform(get("/courses/1/portfolio"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/portfolio"))
                .andExpect(content().string(Matchers.containsString("포트폴리오 제출")));
    }
}
