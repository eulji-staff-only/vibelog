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

/* 화면 URL 로 들어오면 템플릿이 오류 없이 그려지는지 확인한다. (템플릿 문법 오류를 build 에서 잡는다, #52) */
@WebMvcTest(DiagnosisPageController.class)
@WithMockUser   // @WebMvcTest 는 SecurityConfig 를 읽지 않고 기본 보안으로 모든 주소를 막는다 (CLAUDE.md 화면 선행 작업)
class DiagnosisPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 진단_설문_화면을_건너뛰기_확인_모달과_함께_연다() throws Exception {
        mockMvc.perform(get("/diagnosis"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/diagnosis"))
                .andExpect(content().string(Matchers.containsString("진단 중 나가면 응답이 저장되지 않습니다.")))
                .andExpect(content().string(Matchers.containsString("진단을 건너뛸까요?")))
                .andExpect(content().string(Matchers.containsString("vl-footer")));
    }

    @Test
    void 진단_결과_화면을_연다() throws Exception {
        mockMvc.perform(get("/diagnosis/result"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/diagnosis-result"))
                .andExpect(content().string(Matchers.containsString("추천 이유")))
                .andExpect(content().string(Matchers.containsString("다른 트랙")))
                .andExpect(content().string(Matchers.containsString("vl-footer")));
    }
}
