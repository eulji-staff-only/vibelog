package com.vibelog.global.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.model.dto.RankingItem;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.contest.model.dto.ContestSummary;
import com.vibelog.domain.contest.model.service.ContestReader;
import com.vibelog.domain.message.model.dto.NoticeSummary;
import com.vibelog.domain.message.model.service.MessageReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/* 메인 화면 `/` 이 공통 레이아웃(common/main) 화면 이름을 돌려주는지, 로그인 회원이면 헤더용 닉네임을
   모델에 담는지 확인한다 (#132). 닉네임은 모든 화면에 붙는 HeaderAdvice 가 넣는다 (#362, @WebMvcTest 도 Advice 를 읽는다).
   (DB 연결 없이 도는 슬라이스 테스트) */
@WebMvcTest(MainController.class)
@WithMockUser   // @WebMvcTest 는 SecurityConfig 를 읽지 않고 기본 보안으로 모든 주소를 막는다 (CLAUDE.md 화면 선행 작업)
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberReader memberReader;

    @MockitoBean
    private MessageReader messageReader;

    @MockitoBean
    private CommunityReader communityReader;

    @MockitoBean
    private ContestReader contestReader;

    @Test
    void 메인_화면_요청하면_공통_메인_뷰를_돌려준다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("common/main"));
    }

    @Test
    void 최근_공지와_이번_주_추천_작품을_보여준다() throws Exception {
        given(messageReader.findRecentNotices(3)).willReturn(List.of(
                new NoticeSummary(7L, "11월 점검 안내", LocalDateTime.of(2026, 11, 9, 10, 0))));
        given(communityReader.findWeeklyTop(3)).willReturn(List.of(new RankingItem(1, 42L, "할 일 앱", "코딩곰", 5)));

        mockMvc.perform(get("/"))
                .andExpect(content().string(allOf(containsString("/notices/7"), containsString("11월 점검 안내"),
                        containsString("2026-11-09"), containsString("/community/42"), containsString("할 일 앱 — 코딩곰 (추천 5)"))));
    }

    @Test
    void 이달의_콘테스트_주제와_출품_마감을_보여준다() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        given(contestReader.findCurrent()).willReturn(Optional.of(new ContestSummary(3L, "나만의 할 일 앱",
                start, LocalDateTime.of(2026, 11, 20, 23, 59), start.plusDays(20), start.plusDays(29))));

        mockMvc.perform(get("/"))
                .andExpect(content().string(allOf(containsString("나만의 할 일 앱"), containsString("출품 마감 2026-11-20 23:59"))));
    }

    @Test
    void 공지와_추천_작품과_콘테스트가_없으면_안내_문구를_보여준다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(allOf(containsString("등록된 공지사항이 없습니다"),
                        containsString("이번 주에 추천받은 작품이 아직 없습니다"), containsString("이번 달 콘테스트가 아직 없습니다"))));
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
