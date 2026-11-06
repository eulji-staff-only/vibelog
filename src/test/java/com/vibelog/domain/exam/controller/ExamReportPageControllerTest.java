package com.vibelog.domain.exam.controller;

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
@WebMvcTest(ExamReportPageController.class)
class ExamReportPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 오답_분석_리포트_화면을_연다() throws Exception {
        mockMvc.perform(get("/lessons/1/exam/report").param("attempt", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("exam/report"))
                .andExpect(content().string(Matchers.containsString("이 개념 다시 보기")));
    }
}
