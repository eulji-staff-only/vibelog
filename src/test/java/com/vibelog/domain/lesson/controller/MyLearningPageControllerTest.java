package com.vibelog.domain.lesson.controller;

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

/* 화면 URL 로 들어오면 템플릿이 오류 없이 그려지는지 확인한다. (템플릿 문법 오류를 build 에서 잡는다) */
@WebMvcTest(MyLearningPageController.class)
@WithMockUser   // @WebMvcTest 는 SecurityConfig 를 읽지 않고 기본 보안으로 모든 주소를 막는다 (CLAUDE.md 화면 선행 작업)
class MyLearningPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 내_학습_화면을_연다() throws Exception {
        mockMvc.perform(get("/my/learning"))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/my-learning"))
                .andExpect(content().string(Matchers.containsString("내 학습")));
    }

    @Test
    void tab_쿼리가_있어도_같은_화면을_연다() throws Exception {
        mockMvc.perform(get("/my/learning").queryParam("tab", "completed"))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/my-learning"));
    }

    @Test
    void 코스_완료_화면을_연다() throws Exception {
        mockMvc.perform(get("/courses/1/complete"))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/course-complete"))
                .andExpect(content().string(Matchers.containsString("완료 조건")));
    }
}
