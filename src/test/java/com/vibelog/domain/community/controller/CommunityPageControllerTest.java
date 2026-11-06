package com.vibelog.domain.community.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 커뮤니티 화면(U-21~U-23)이 설계서 URL 로 열리는지 확인한다. 목록은 비회원도 (상세는 CommunityPostReadTest), 글쓰기·수정은 회원만. (#50)
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommunityPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
            "/community, community/list",
            "/community?category=SHOWCASE&page=2, community/list"
    })
    void 비회원도_목록을_연다(String url, String viewName) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk()).andExpect(view().name(viewName));
    }

    @Test
    void 비회원이_글쓰기에_오면_로그인으로_보낸다() throws Exception {
        mockMvc.perform(get("/community/write")).andExpect(redirectedUrl("/login?redirect=%2Fcommunity%2Fwrite"));
        mockMvc.perform(get("/community/1/edit")).andExpect(redirectedUrl("/login?redirect=%2Fcommunity%2F1%2Fedit"));
    }

    @ParameterizedTest
    @WithMockUser
    @CsvSource({"/community/write", "/community/1/edit"})
    void 회원은_글쓰기와_수정을_연다(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk()).andExpect(view().name("community/write"));
    }
}
