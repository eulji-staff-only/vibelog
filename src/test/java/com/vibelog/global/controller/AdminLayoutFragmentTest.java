package com.vibelog.global.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* 백오피스 왼쪽 메뉴 fragment(common/admin-layout.html :: sidebar)가 화면 흐름 설계서의 메뉴·URL 그대로
   렌더링되고, 지금 보고 있는 메뉴가 강조되는지 확인한다 (#81). 실제 A-01~A-08 화면은 다른 이슈
   범위라, 여기서는 테스트 전용 빈 화면(admintest/sample, AdminTestSampleController)에 fragment 를
   씌워 확인한다. (DB 연결 없이 도는 슬라이스 테스트) */
@WebMvcTest(controllers = AdminTestSampleController.class)
@WithMockUser(roles = "ADMIN")   // @WebMvcTest 는 SecurityConfig 를 읽지 않으므로 직접 인증 상태를 준다
class AdminLayoutFragmentTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 왼쪽_메뉴에_설계서_URL이_모두_있다() throws Exception {
        mockMvc.perform(get("/admintest/sample").param("active", "dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/admin\"")))
                .andExpect(content().string(containsString("href=\"/admin/tracks\"")))
                .andExpect(content().string(containsString("href=\"/admin/categories\"")))
                .andExpect(content().string(containsString("href=\"/admin/courses\"")))
                .andExpect(content().string(containsString("href=\"/admin/members\"")))
                .andExpect(content().string(containsString("학습자 화면으로")))
                .andExpect(content().string(containsString("href=\"/\"")));
    }

    /* 설정(P2) 메뉴는 화면이 생긴 것만 둔다: 공지(A-10, #186). 진단·콘테스트 화면은 아직 없다 */
    @Test
    void 설정_메뉴에는_화면이_있는_공지만_있다() throws Exception {
        mockMvc.perform(get("/admintest/sample").param("active", "dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/admin/notices\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/admin/contests"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/admin/diagnosis"))));
    }

    @Test
    void 지금_보고_있는_메뉴를_강조한다() throws Exception {
        mockMvc.perform(get("/admintest/sample").param("active", "tracks"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "class=\"vl-admin__link is-active\" href=\"/admin/tracks\"")));
    }
}
