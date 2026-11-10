package com.vibelog.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* 점검 중(MAINTENANCE=true)이면 모든 요청이 503 + E-04, 점검 화면이 쓰는 CSS 는 그대로 (#365, PL-ERR-006) */
@SpringBootTest(properties = "vibelog.maintenance=true")
@AutoConfigureMockMvc
class MaintenanceFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 화면은_점검_중_화면_API_는_JSON_으로_503_을_보낸다() throws Exception {
        mockMvc.perform(get("/community"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("서비스 점검 중입니다")))
                .andExpect(content().string(containsString("support@vibelog.example")));
        mockMvc.perform(get("/mypage"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MAINTENANCE"))
                .andExpect(jsonPath("$.message").value("서비스 점검 중입니다."));
    }

    @Test
    void 점검_화면이_쓰는_CSS_는_보낸다() throws Exception {
        mockMvc.perform(get("/css/common.css")).andExpect(status().isOk());
    }
}
