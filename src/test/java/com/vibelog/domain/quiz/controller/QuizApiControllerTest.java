package com.vibelog.domain.quiz.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.quiz.exception.QuizErrorCode;
import com.vibelog.domain.quiz.model.dto.QuizCheckResponse;
import com.vibelog.domain.quiz.model.dto.QuizStartResponse;
import com.vibelog.domain.quiz.model.dto.QuizSubmitResponse;
import com.vibelog.domain.quiz.model.service.QuizSolveService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* 퀴즈 JSON API (API-LSN-04~06, #108): 응답 모양, 정답이 새지 않는지, 실패가 {code, message} 인지 */
@WebMvcTest(QuizApiController.class)
class QuizApiControllerTest {

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
    void 풀이_시작_응답에는_문항과_보기만_있고_정답은_없다() throws Exception {
        given(quizSolveService.start(7L, 3L)).willReturn(new QuizStartResponse(100L, List.of(
                new QuizStartResponse.Question(11L, "CONCEPT", "문항", null,
                        List.of(new QuizStartResponse.Choice(111L, "보기1"), new QuizStartResponse.Choice(112L, "보기2"))))));

        mockMvc.perform(post("/api/quizzes/3/attempts").with(member()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizAttemptId").value(100))
                .andExpect(jsonPath("$.questions[0].displayType").value("CONCEPT"))
                .andExpect(jsonPath("$.questions[0].choices[1].quizChoiceId").value(112))
                .andExpect(content().string(Matchers.not(Matchers.containsStringIgnoringCase("correct"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("explanation"))));
    }

    @Test
    void 문항_확인은_정답과_해설을_준다() throws Exception {
        given(quizSolveService.check(7L, 100L, 11L, 112L)).willReturn(new QuizCheckResponse(false, 111L, "해설"));

        mockMvc.perform(post("/api/quiz-attempts/100/answers").with(member()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizQuestionId\": 11, \"quizChoiceId\": 112}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.correctQuizChoiceId").value(111))
                .andExpect(jsonPath("$.explanation").value("해설"));
    }

    @Test
    void 이미_확인한_문항이면_409_JSON() throws Exception {
        given(quizSolveService.check(7L, 100L, 11L, 111L)).willThrow(new BusinessException(QuizErrorCode.ALREADY_CHECKED));

        mockMvc.perform(post("/api/quiz-attempts/100/answers").with(member()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizQuestionId\": 11, \"quizChoiceId\": 111}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CHECKED"))
                .andExpect(jsonPath("$.message").value("이미 확인한 문항입니다."));
    }

    @Test
    void 남의_풀이면_403_JSON() throws Exception {
        given(quizSolveService.submit(7L, 200L)).willThrow(new BusinessException(CommonErrorCode.NOT_OWNER));

        mockMvc.perform(post("/api/quiz-attempts/200/submit").with(member()).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNER"));
    }

    @Test
    void 제출하면_정답률_통과_다음_위치를_준다() throws Exception {
        given(quizSolveService.submit(7L, 100L)).willReturn(new QuizSubmitResponse(new BigDecimal("75.00"), true,
                List.of(new QuizSubmitResponse.TagStat("JSP 표현식", 50, true, 8L)),
                new QuizSubmitResponse.Next("CONCEPT", "/courses/1/lessons/2?activity=5"), "다음 개념이 열렸습니다"));

        mockMvc.perform(post("/api/quiz-attempts/100/submit").with(member()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctRate").value(75.00))
                .andExpect(jsonPath("$.isPassed").value(true))
                .andExpect(jsonPath("$.tagStats[0].conceptTagName").value("JSP 표현식"))
                .andExpect(jsonPath("$.tagStats[0].wrongRate").value(50))
                .andExpect(jsonPath("$.tagStats[0].weak").value(true))
                .andExpect(jsonPath("$.tagStats[0].conceptActivityId").value(8))
                .andExpect(jsonPath("$.next.type").value("CONCEPT"))
                .andExpect(jsonPath("$.next.url").value("/courses/1/lessons/2?activity=5"))
                .andExpect(jsonPath("$.message").value("다음 개념이 열렸습니다"));
    }
}
