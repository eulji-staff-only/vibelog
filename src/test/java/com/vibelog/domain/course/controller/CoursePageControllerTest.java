package com.vibelog.domain.course.controller;

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

/* 화면 URL 로 들어오면 템플릿이 오류 없이 그려지는지 확인한다. (템플릿 문법 오류를 build 에서 잡는다, #53) */
@WebMvcTest(CoursePageController.class)
@WithMockUser   // @WebMvcTest 는 SecurityConfig 를 읽지 않고 기본 보안으로 모든 주소를 막는다 (CLAUDE.md 화면 선행 작업)
class CoursePageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 트랙_소개_화면을_연다() throws Exception {
        mockMvc.perform(get("/tracks/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/track"))
                .andExpect(content().string(Matchers.containsString("이 트랙의 코스")))
                .andExpect(content().string(Matchers.containsString("vl-header")))
                .andExpect(content().string(Matchers.containsString("vl-footer")));
    }

    @Test
    void 코스_목록_화면을_연다() throws Exception {
        mockMvc.perform(get("/courses").param("track", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/list"))
                .andExpect(content().string(Matchers.containsString("조건에 맞는 코스가 없습니다")))
                .andExpect(content().string(Matchers.containsString("vl-footer")));
    }

    @Test
    void 코스_상세_화면을_연다() throws Exception {
        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/detail"))
                .andExpect(content().string(Matchers.containsString("수강 시작")))
                .andExpect(content().string(Matchers.containsString("수업 목록")))
                .andExpect(content().string(Matchers.containsString("vl-footer")));
    }
}
