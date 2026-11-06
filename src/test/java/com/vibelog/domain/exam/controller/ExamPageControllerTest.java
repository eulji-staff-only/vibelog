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
@WebMvcTest(ExamPageController.class)
class ExamPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 모의고사_응시_화면을_연다() throws Exception {
        mockMvc.perform(get("/lessons/1/exam"))
                .andExpect(status().isOk())
                .andExpect(view().name("exam/exam"))
                .andExpect(content().string(Matchers.containsString("모의고사를 제출할까요?")));
    }
}
