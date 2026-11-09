package com.vibelog.domain.quiz.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.quiz.model.dto.WrongAnswerView;
import com.vibelog.domain.quiz.model.service.QuizWrongAnswerService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/* 틀린 문제 모아보기 화면 (U-28, API-LSN-10, #190) */
@WebMvcTest(QuizWrongAnswerController.class)
class QuizWrongAnswerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QuizWrongAnswerService quizWrongAnswerService;

    private RequestPostProcessor member() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(7L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }

    @Test
    void 필터와_쪽을_넘기고_틀린_문항과_정답과_개념_링크를_그린다() throws Exception {
        WrongAnswerView.Item item = new WrongAnswerView.Item(1L, "JSP 코스", "수업 1", "결과 유추하기", "출력은?", "print(1)",
                "2", "1", "1을 출력한다", "출력", "/courses/3/lessons/5?activity=8");
        given(quizWrongAnswerService.getPage(eq(7L), eq(3L), eq(5L), eq(1))).willReturn(new WrongAnswerView(List.of(item), 1, 1, 1,
                List.of(new WrongAnswerView.Option(3L, "JSP 코스", 3L)), List.of(new WrongAnswerView.Option(5L, "수업 1", 3L)), 3L, 5L));

        mockMvc.perform(get("/my/wrong-answers").with(member()).param("course", "3").param("lesson", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("quiz/wrong-answers"))
                .andExpect(content().string(Matchers.containsString("출력은?")))
                .andExpect(content().string(Matchers.containsString("print(1)")))
                .andExpect(content().string(Matchers.containsString("1을 출력한다")))
                .andExpect(content().string(Matchers.containsString("href=\"/courses/3/lessons/5?activity=8\"")))
                .andExpect(content().string(Matchers.containsString("조건 초기화")));
    }

    @Test
    void 틀린_문제가_없으면_안내_상자를_보여준다() throws Exception {
        given(quizWrongAnswerService.getPage(eq(7L), eq(null), eq(null), eq(1)))
                .willReturn(new WrongAnswerView(List.of(), 1, 1, 0, List.of(), List.of(), null, null));

        mockMvc.perform(get("/my/wrong-answers").with(member()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("틀린 문제가 없습니다")));
    }
}
