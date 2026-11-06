package com.vibelog.global.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.service.MemberReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/* 메인 화면 `/` 이 공통 레이아웃(common/main) 화면 이름을 돌려주는지, 로그인 회원이면 헤더용 닉네임을
   모델에 담는지 확인한다 (#132). (DB 연결 없이 도는 슬라이스 테스트) */
@WebMvcTest(MainController.class)
@WithMockUser   // @WebMvcTest 는 SecurityConfig 를 읽지 않고 기본 보안으로 모든 주소를 막는다 (CLAUDE.md 화면 선행 작업)
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberReader memberReader;

    @Test
    void 메인_화면_요청하면_공통_메인_뷰를_돌려준다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("common/main"));
    }

    @Test
    void 로그인한_회원이면_닉네임을_모델에_담는다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        given(memberReader.getNickname(1L)).willReturn("테스트학습자");

        mockMvc.perform(get("/").with(authentication(
                        new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("loginNickname", "테스트학습자"));
    }
}
