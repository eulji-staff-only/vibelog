package com.vibelog.domain.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 구글 키만 넣었을 때 (#91): 구글 버튼만 보이고, 버튼 주소(API-USR-06)가 구글 로그인 화면으로 보낸다.
 * 테스트용 가짜 키라 실제 구글 로그인은 하지 않는다.
 */
@SpringBootTest(properties = {
        "vibelog.oauth.google-client-id=test-google-id",
        "vibelog.oauth.google-client-secret=test-google-secret"
})
@AutoConfigureMockMvc
class SocialLoginEnabledTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 키가_있는_제공자만_버튼이_보이고_구글_로그인_화면으로_보낸다() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(allOf(
                        containsString("href=\"/oauth2/authorization/google\""),
                        not(containsString("/oauth2/authorization/github")))));

        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string("Location", containsString("client_id=test-google-id")));
    }
}
