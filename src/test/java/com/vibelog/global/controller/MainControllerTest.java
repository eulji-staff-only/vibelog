package com.vibelog.global.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 메인 화면 `/` 이 공통 레이아웃(common/main) 화면 이름을 돌려주는지 확인한다. (DB 연결 없이 도는 슬라이스 테스트)
 * addFilters = false: 실제 SecurityConfig는 "/"를 permitAll로 열어두지만(#12),
 * 이 컨트롤러 슬라이스 테스트는 그 설정을 로드하지 않아 기본 보안 필터가 모든 요청을 막는다.
 * 여기서 확인하려는 것은 권한이 아니라 메인 화면 매핑이므로 보안 필터만 끈다.
 */
@WebMvcTest(MainController.class)
@AutoConfigureMockMvc(addFilters = false)
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 메인_화면_요청하면_공통_메인_뷰를_돌려준다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("common/main"));
    }
}
