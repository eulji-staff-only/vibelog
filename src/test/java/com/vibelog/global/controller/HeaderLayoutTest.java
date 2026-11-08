package com.vibelog.global.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 공통 헤더에 모바일 햄버거 버튼과 펼치기 스크립트가 들어 있는지 확인한다 (설계서 7장, #153).
 * 헤더 조각을 쓰는 모든 화면에 함께 들어간다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HeaderLayoutTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 헤더에_햄버거_버튼과_스크립트가_있다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("aria-controls=\"vl-header-menu\""),
                        containsString("aria-expanded=\"false\""),
                        containsString("id=\"vl-header-menu\""),
                        containsString("/js/header.js"))));
    }
}
