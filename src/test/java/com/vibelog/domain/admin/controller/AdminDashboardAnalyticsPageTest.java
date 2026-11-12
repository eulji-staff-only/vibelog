package com.vibelog.domain.admin.controller;

import com.vibelog.domain.admin.model.dto.AdminLearningAnalytics;
import com.vibelog.domain.admin.model.service.AdminLearningAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 대시보드 2차 지표 화면 (A-01, API-ADM-40, FR-ADM-003, #194). 지표 값은 AdminLearningAnalyticsService 를 대신해 넣어
 * 화면이 값과 문구를 제대로 그리는지만 확인한다 (집계 규칙은 Reader·Service 테스트가 확인한다).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminDashboardAnalyticsPageTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminLearningAnalyticsService analyticsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void 지표가_있으면_네_구역의_값과_약한_개념_표시를_그린다() throws Exception {
        given(analyticsService.getAnalytics(any(), any())).willReturn(new AdminLearningAnalytics(
                List.of(new AdminLearningAnalytics.TrackRecommendation(1L, "웹 트랙", 4, 3, 75)),
                List.of(new AdminLearningAnalytics.LessonDropOff(1L, "웹 코스", "첫 수업", 10, 4, 40)),
                List.of(new AdminLearningAnalytics.ActivityDropOff(7L, "웹 코스", "첫 수업", "변수 개념", 8, 6, 75)),
                new AdminLearningAnalytics.QuizPassStat(8, 6, 75),
                List.of(new AdminLearningAnalytics.ConceptWrongRate(1L, "반복문", 12, 7, 58, true),
                        new AdminLearningAnalytics.ConceptWrongRate(2L, "변수", 10, 4, 40, false)),
                List.of(new AdminLearningAnalytics.ExamLessonStat(1L, "웹 코스", "첫 수업", 60.2, List.of("반복문", "함수")),
                        new AdminLearningAnalytics.ExamLessonStat(2L, "웹 코스", "둘째 수업", 80.0, List.of()))));

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("추천 트랙과 실제 수강"),
                        containsString("웹 트랙"),
                        containsString("4명"),
                        containsString("75%"),
                        containsString("수업별 이탈률"),
                        containsString("첫 수업"),
                        containsString("40%"),
                        // #364: 수업별 표 아래에 활동별 이탈 표가 함께 보인다
                        containsString("활동별 이탈률"),
                        containsString("변수 개념"),
                        containsString("퀴즈 통과율 75% (응시 8건 중 통과 6건)"),
                        containsString("반복문"),
                        containsString("58%"),
                        containsString("약한 개념"),
                        containsString("모의고사 평균·약한 개념"),
                        containsString("60.2점"),
                        containsString("반복문, 함수"),
                        containsString("없음"),
                        not(containsString("집계된 데이터가 없습니다")))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 지표가_없으면_구역마다_빈_상태_문구를_보여준다() throws Exception {
        given(analyticsService.getAnalytics(any(), any())).willReturn(new AdminLearningAnalytics(
                List.of(), List.of(), List.of(), new AdminLearningAnalytics.QuizPassStat(0, 0, 0), List.of(), List.of()));

        String body = mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("추천 트랙과 실제 수강"),
                        containsString("수업별 이탈률"),
                        containsString("활동별 이탈률"),
                        containsString("퀴즈 통과율·개념별 오답률"),
                        containsString("모의고사 평균·약한 개념"))))
                .andReturn().getResponse().getContentAsString();

        // 추천 트랙, 수업별 이탈률, 활동별 이탈률, 퀴즈 통과율, 개념별 오답률, 모의고사 여섯 곳
        org.assertj.core.api.Assertions.assertThat(body.split("집계된 데이터가 없습니다", -1).length - 1).isEqualTo(6);
    }
}
