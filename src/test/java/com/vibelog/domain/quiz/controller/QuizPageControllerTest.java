package com.vibelog.domain.quiz.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/* 화면 URL 로 들어오면 템플릿이 오류 없이 그려지는지 확인한다. (템플릿 문법 오류를 build 에서 잡는다, #54) */
@WebMvcTest(QuizPageController.class)
@WithMockUser   // @WebMvcTest 는 SecurityConfig 를 읽지 않고 기본 보안으로 모든 주소를 막는다 (CLAUDE.md 화면 선행 작업)
class QuizPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 퀴즈_화면을_표시_유형_4가지와_채점_결과_영역까지_연다() throws Exception {
        mockMvc.perform(get("/courses/1/lessons/1/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("quiz/quiz"))
                .andExpect(content().string(Matchers.containsString("코드 빈칸 채우기")))
                .andExpect(content().string(Matchers.containsString("개념 문제")))
                .andExpect(content().string(Matchers.containsString("결과 유추하기")))
                .andExpect(content().string(Matchers.containsString("낚시 문제")))
                .andExpect(content().string(Matchers.containsString("개념 태그별 오답 비율")))
                .andExpect(content().string(Matchers.containsString("vl-footer")));
    }
}
