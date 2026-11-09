package com.vibelog.domain.course.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.course.model.dto.CourseCard;
import com.vibelog.domain.course.model.dto.DiagnosisResultView;
import com.vibelog.domain.course.model.dto.DiagnosisSurveyView;
import com.vibelog.domain.course.model.dto.TrackIntroView;
import com.vibelog.domain.course.model.service.DiagnosisService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/* 진단 설문·제출·건너뛰기·결과 화면 (U-07, M-01, U-08, #52·#99) */
@WebMvcTest(DiagnosisPageController.class)
class DiagnosisPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DiagnosisService diagnosisService;

    private RequestPostProcessor member() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(7L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }

    @Test
    void 진단_설문은_문항과_보기와_제출_폼을_그린다() throws Exception {
        given(diagnosisService.getSurvey()).willReturn(new DiagnosisSurveyView(List.of(
                new DiagnosisSurveyView.Question(1L, "AI에게 코드를 맡겨 본 경험은?",
                        List.of(new DiagnosisSurveyView.Option(11L, "처음이에요"), new DiagnosisSurveyView.Option(12L, "몇 번 해 봤어요"))))));

        mockMvc.perform(get("/diagnosis").with(member()))
                .andExpect(status().isOk())
                .andExpect(view().name("course/diagnosis"))
                .andExpect(content().string(Matchers.containsString("AI에게 코드를 맡겨 본 경험은?")))
                .andExpect(content().string(Matchers.containsString("data-option-id=\"12\"")))
                .andExpect(content().string(Matchers.containsString("name=\"diagnosisOptionIds\"")))
                .andExpect(content().string(Matchers.containsString("action=\"/diagnosis/skip\"")))
                .andExpect(content().string(Matchers.containsString("진단을 건너뛸까요?")));
    }

    @Test
    void 문항이_없으면_안내_상자를_보여준다() throws Exception {
        given(diagnosisService.getSurvey()).willReturn(new DiagnosisSurveyView(List.of()));

        mockMvc.perform(get("/diagnosis").with(member()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("아직 준비된 진단 문항이 없습니다")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("action=\"/diagnosis\""))));
    }

    @Test
    void 제출하면_추천_이유를_flash로_넘기고_결과로_간다() throws Exception {
        given(diagnosisService.submit(eq(7L), eq(List.of(11L, 21L))))
                .willReturn(new DiagnosisService.SubmitResult(2L, List.of("범위가 자꾸 넓어져요")));

        mockMvc.perform(post("/diagnosis").with(member()).with(csrf())
                        .param("diagnosisOptionIds", "11", "21"))
                .andExpect(redirectedUrl("/diagnosis/result"))
                .andExpect(flash().attribute("reasons", List.of("범위가 자꾸 넓어져요")));
    }

    @Test
    void 건너뛰면_코스_목록으로_간다() throws Exception {
        mockMvc.perform(post("/diagnosis/skip").with(member()).with(csrf()))
                .andExpect(redirectedUrl("/courses"));

        verify(diagnosisService).skip(7L);
    }

    @Test
    void 결과가_없거나_건너뛰었으면_설문으로_보낸다() throws Exception {
        given(diagnosisService.getResult(eq(7L), any())).willReturn(Optional.empty());

        mockMvc.perform(get("/diagnosis/result").with(member()))
                .andExpect(redirectedUrl("/diagnosis"));
    }

    @Test
    void 결과_화면은_추천_트랙과_이유와_다른_트랙을_그린다() throws Exception {
        TrackIntroView recommended = new TrackIntroView(2L, "Controller", 2, "통제가 필요한 학습자", "트랙 설명",
                List.of(new CourseCard(1L, 2L, "AI 작업 범위 통제하기", "한 줄", "Controller", "프롬프트", 14, 4)));
        given(diagnosisService.getResult(eq(7L), any())).willReturn(Optional.of(new DiagnosisResultView(recommended,
                List.of("범위가 자꾸 넓어져요"),
                List.of(new DiagnosisResultView.OtherTrack(1L, "Starter", "입문 학습자", "입문 설명")))));

        mockMvc.perform(get("/diagnosis/result").with(member()))
                .andExpect(status().isOk())
                .andExpect(view().name("course/diagnosis-result"))
                .andExpect(content().string(Matchers.containsString("2단계")))
                .andExpect(content().string(Matchers.containsString("범위가 자꾸 넓어져요")))
                .andExpect(content().string(Matchers.containsString("AI 작업 범위 통제하기")))
                .andExpect(content().string(Matchers.containsString("href=\"/tracks/1\"")));
    }
}
