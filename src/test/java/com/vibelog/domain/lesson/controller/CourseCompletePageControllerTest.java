package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.model.dto.CourseCompleteCondition;
import com.vibelog.domain.lesson.model.dto.CourseCompleteLessonReport;
import com.vibelog.domain.lesson.model.dto.CourseCompleteView;
import com.vibelog.domain.lesson.model.service.CourseCompleteService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/* 코스 완료 화면(U-18, API-ENR-03) 요청이 로그인 회원의 memberId 만 Service 에 넘기고,
   완료·남음 두 상태와 수강하지 않은 경우(코스 상세로 redirect)를 모두 맞게 그리는지 확인한다
   (DB 연결 없이 도는 슬라이스 테스트). */
@WebMvcTest(CourseCompletePageController.class)
class CourseCompletePageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseCompleteService courseCompleteService;

    @Test
    void 완료_조건을_모두_채우면_완료_화면을_보여준다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);

        CourseCompleteView view = new CourseCompleteView(
                1L, "테스트 트랙", "테스트 코스", true, LocalDateTime.of(2026, 10, 20, 14, 32),
                List.of(
                        new CourseCompleteCondition("모든 공개 수업 모의고사 응시", true),
                        new CourseCompleteCondition("모든 공개 수업 오답 분석 리포트 확인", true),
                        new CourseCompleteCondition("포트폴리오 제출", true)
                ),
                List.of(new CourseCompleteLessonReport(1L, "수업 1", "/lessons/1/exam/report?attempt=1"))
        );
        given(courseCompleteService.getCourseComplete(1L, 1L)).willReturn(Optional.of(view));

        mockMvc.perform(get("/courses/1/complete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/course-complete"))
                .andExpect(content().string(Matchers.containsString("코스를 완료했습니다")))
                .andExpect(content().string(Matchers.containsString("완료 조건")))
                // 평가 에이전트는 제출 뒤 내 포트폴리오 > 수정에서 열린다 (#404)
                .andExpect(content().string(Matchers.containsString("채점 에이전트는 내 포트폴리오 &gt; 수정에 들어가서 확인할 수 있습니다.")));
    }

    @Test
    void 완료_조건이_남으면_한_걸음_남았어요를_보여준다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);

        CourseCompleteView view = new CourseCompleteView(
                1L, "테스트 트랙", "테스트 코스", false, null,
                List.of(
                        new CourseCompleteCondition("모든 공개 수업 모의고사 응시", true),
                        new CourseCompleteCondition("모든 공개 수업 오답 분석 리포트 확인", true),
                        new CourseCompleteCondition("포트폴리오 제출", false)
                ),
                List.of(new CourseCompleteLessonReport(1L, "수업 1", "/lessons/1/exam/report?attempt=1"))
        );
        given(courseCompleteService.getCourseComplete(1L, 1L)).willReturn(Optional.of(view));

        mockMvc.perform(get("/courses/1/complete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/course-complete"))
                .andExpect(content().string(Matchers.containsString("한 걸음 남았어요")))
                // 아직 완료 전이면 에이전트 안내를 보여 주지 않는다 (제출해야 열린다)
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-agent-hint"))))
                // 설계서 U-18 예외: 남은 조건 바로가기는 포트폴리오(U-17)로 보낸다 (#360)
                .andExpect(content().string(Matchers.containsString("href=\"/courses/1/portfolio\">포트폴리오로 가기")));
    }

    @Test
    void 완료했으면_남은_조건_바로가기를_보여주지_않는다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        CourseCompleteView view = new CourseCompleteView(
                1L, "테스트 트랙", "테스트 코스", true, LocalDateTime.of(2026, 10, 20, 14, 32),
                List.of(new CourseCompleteCondition("포트폴리오 제출", true)), List.of());
        given(courseCompleteService.getCourseComplete(1L, 1L)).willReturn(Optional.of(view));

        mockMvc.perform(get("/courses/1/complete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("포트폴리오로 가기"))));
    }

    @Test
    void 같은_코스_작품_보기_링크가_코스_작품_목록_주소로_그려진다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        CourseCompleteView view = new CourseCompleteView(
                7L, "테스트 트랙", "테스트 코스", true, LocalDateTime.of(2026, 10, 20, 14, 32),
                List.of(new CourseCompleteCondition("포트폴리오 제출", true)), List.of());
        given(courseCompleteService.getCourseComplete(1L, 7L)).willReturn(Optional.of(view));

        mockMvc.perform(get("/courses/7/complete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.allOf(
                        Matchers.containsString("같은 코스 작품 보기"),
                        Matchers.containsString("href=\"/courses/7/portfolios\""))));
    }

    @Test
    void 수강하지_않았으면_코스_상세로_돌려보낸다() throws Exception {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        given(courseCompleteService.getCourseComplete(1L, 1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/courses/1/complete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1"));
    }
}
