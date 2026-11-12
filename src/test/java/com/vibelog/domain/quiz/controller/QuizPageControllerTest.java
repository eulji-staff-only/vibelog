package com.vibelog.domain.quiz.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.quiz.model.dto.QuizPageView;
import com.vibelog.domain.quiz.model.service.QuizSolveService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/* 퀴즈 화면 (U-14, API-LSN-03, #54·#108) */
@WebMvcTest(QuizPageController.class)
class QuizPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QuizSolveService quizSolveService;

    private RequestPostProcessor member() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(7L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }

    @Test
    void 열린_퀴즈는_제목_최고점수와_풀이_시작_주소를_담아_그린다() throws Exception {
        given(quizSolveService.getPage(7L, 3L)).willReturn(Optional.of(
                new QuizPageView(1L, 2L, 3L, "JSP 기초 퀴즈", 4, new BigDecimal("75.00"))));

        mockMvc.perform(get("/courses/1/lessons/2/quizzes/3").with(member()))
                .andExpect(status().isOk())
                .andExpect(view().name("quiz/quiz"))
                .andExpect(content().string(Matchers.containsString("JSP 기초 퀴즈")))
                .andExpect(content().string(Matchers.containsString("최고 점수 75%")))
                .andExpect(content().string(Matchers.containsString("data-start-url=\"/api/quizzes/3/attempts\"")))
                .andExpect(content().string(Matchers.containsString("name=\"_csrf\"")))
                .andExpect(content().string(Matchers.containsString("1 / 4")));
    }

    @Test
    void 닫힌_퀴즈는_수업_화면으로_보내고_잠김_안내를_띄운다() throws Exception {
        given(quizSolveService.getPage(7L, 3L)).willReturn(Optional.empty());

        mockMvc.perform(get("/courses/1/lessons/2/quizzes/3").with(member()))
                .andExpect(redirectedUrl("/courses/1/lessons/2"))
                .andExpect(flash().attribute("toast", "앞 퀴즈를 통과하면 열립니다."));
    }

    @Test
    void 문항이_없는_퀴즈는_안내_상자를_보여준다() throws Exception {
        given(quizSolveService.getPage(7L, 3L)).willReturn(Optional.of(new QuizPageView(1L, 2L, 3L, "빈 퀴즈", 0, null)));

        mockMvc.perform(get("/courses/1/lessons/2/quizzes/3").with(member()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("아직 준비된 문항이 없습니다")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-quiz-solve"))));
    }
}
