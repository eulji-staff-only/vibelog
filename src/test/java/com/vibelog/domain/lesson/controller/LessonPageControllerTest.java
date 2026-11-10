package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dto.ActivityStatus;
import com.vibelog.domain.lesson.model.dto.CompleteActivityResult;
import com.vibelog.domain.lesson.model.dto.LessonActivityCard;
import com.vibelog.domain.lesson.model.dto.LessonPageData;
import com.vibelog.domain.lesson.model.dto.LessonPageResult;
import com.vibelog.domain.lesson.model.service.LessonActivityService;
import com.vibelog.domain.lesson.model.service.LessonPageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.FlashMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * API-LSN-01·02 를 Service 는 모킹하고 Controller 흐름(로그인 회원 꺼내기 · redirect · 모델 전달)만 확인한다
 * (DB 연결 없이 도는 슬라이스 테스트). 데이터 계산 자체는 LessonPageServiceTest·LessonActivityServiceTest(실제 DB) 참고.
 */
@WebMvcTest(LessonPageController.class)
class LessonPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LessonPageService lessonPageService;

    @MockitoBean
    LessonActivityService lessonActivityService;

    private UsernamePasswordAuthenticationToken loginAs(long memberId) {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(memberId);
        return new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER")));
    }

    @Test
    void 열린_수업이면_수업_화면을_그린다() throws Exception {
        LessonActivityCard quiz = new LessonActivityCard(2L, LessonActivityCard.QUIZ, "퀴즈 1",
                ActivityStatus.IN_PROGRESS, true, null, null, null, null, null, null, null, null);
        LessonPageData data = new LessonPageData(1L, 1L, "수업 1", "목표", List.of(quiz));
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.ok(data));

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/lesson"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("활동 목록")));
    }

    @Test
    void 잠긴_활동으로_돌아오면_수업_화면에_안내_토스트를_그린다() throws Exception {
        LessonActivityCard quiz = new LessonActivityCard(2L, LessonActivityCard.QUIZ, "퀴즈 1",
                ActivityStatus.IN_PROGRESS, true, null, null, null, null, null, null, null, null);
        LessonPageData data = new LessonPageData(1L, 1L, "수업 1", "목표", List.of(quiz));
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.ok(data));

        mockMvc.perform(get("/courses/1/lessons/1")
                        .with(authentication(loginAs(1L)))
                        .flashAttr("toast", LessonErrorCode.ACTIVITY_LOCKED.getMessage()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "role=\"status\">" + LessonErrorCode.ACTIVITY_LOCKED.getMessage())));
    }

    @Test
    void 수업_화면에도_다른_화면과_같은_공통_헤더와_푸터가_그려진다() throws Exception {
        LessonActivityCard quiz = new LessonActivityCard(2L, LessonActivityCard.QUIZ, "퀴즈 1",
                ActivityStatus.IN_PROGRESS, true, null, null, null, null, null, null, null, null);
        LessonPageData data = new LessonPageData(1L, 1L, "수업 1", "목표", List.of(quiz));
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.ok(data));

        // #358: 이 화면만 헤더가 없어 알림·공지 배너·메뉴가 안 보이던 문제
        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("vl-header"),
                        org.hamcrest.Matchers.containsString("vl-footer"))));
    }

    @Test
    void 잠금_안내와_퀴즈_설명에는_고정_통과_비율을_쓰지_않는다() throws Exception {
        // 통과 기준이 활동마다(activity.pass_rate) 달라서 "정답률 50%" 같은 고정 문구가 남으면 안내가 틀린다 (#277, #358)
        LessonActivityCard lockedConcept = new LessonActivityCard(1L, LessonActivityCard.CONCEPT, "개념 1",
                ActivityStatus.LOCKED, false, null, null, List.of(), null, null, null, null, null);
        LessonActivityCard quiz = new LessonActivityCard(2L, LessonActivityCard.QUIZ, "퀴즈 1",
                ActivityStatus.IN_PROGRESS, true, null, null, null, null, null, null, null, null);
        LessonPageData data = new LessonPageData(1L, 1L, "수업 1", "목표", List.of(lockedConcept, quiz));
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.ok(data));

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("앞 퀴즈를 통과하면 열립니다."),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("50%")))));
        assertThat(LessonErrorCode.ACTIVITY_LOCKED.getMessage()).isEqualTo("앞 퀴즈를 통과하면 열립니다.");
    }

    private UsernamePasswordAuthenticationToken loginAsAdmin(long memberId) {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(memberId);
        given(loginMember.isAdmin()).willReturn(true);
        return new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private LessonPageData previewData() {
        LessonActivityCard concept = new LessonActivityCard(1L, LessonActivityCard.CONCEPT, "개념 1",
                ActivityStatus.IN_PROGRESS, true, "본문", null, List.of(), null, null, null, null, null);
        LessonActivityCard quiz = new LessonActivityCard(2L, LessonActivityCard.QUIZ, "퀴즈 1",
                ActivityStatus.IN_PROGRESS, false, null, null, null, null, null, null, null, null);
        LessonActivityCard exam = new LessonActivityCard(null, LessonActivityCard.EXAM, "모의고사",
                ActivityStatus.IN_PROGRESS, false, null, null, null, null, null, null, null, null);
        return new LessonPageData(1L, 1L, "수업 1", "목표", List.of(concept, quiz, exam), true);
    }

    @Test
    void 운영자가_열면_미리보기로_Service에_넘기고_안내를_보여주며_기록_버튼은_숨긴다() throws Exception {
        given(lessonPageService.getPage(2L, 1L, 1L, null, true)).willReturn(LessonPageResult.ok(previewData()));

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAsAdmin(2L))))
                .andExpect(status().isOk())
                .andExpect(view().name("lesson/lesson"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("운영자 미리보기입니다. 진행 기록이 저장되지 않습니다."),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("읽기 완료 · 퀴즈로")),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-bookmark")),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("모의고사 응시하기")),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("퀴즈 풀기")))));
    }

    private LessonPageData examPageData(boolean preview) {
        LessonActivityCard exam = new LessonActivityCard(null, LessonActivityCard.EXAM, "모의고사",
                ActivityStatus.IN_PROGRESS, true, null, null, null, null, null, null,
                "/lessons/1/exam/attempts", null, List.of(), false, List.of(), "/lessons/1/exam/report?attempt=2");
        return new LessonPageData(1L, 1L, "수업 1", "목표", List.of(exam), preview);
    }

    @Test
    void 제출한_회차가_있으면_모의고사_활동에_지난_리포트_다시_보기가_보인다() throws Exception {
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.ok(examPageData(false)));

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("지난 리포트 다시 보기"),
                        org.hamcrest.Matchers.containsString("href=\"/lessons/1/exam/report?attempt=2\""))));
    }

    @Test
    void 제출한_회차가_없으면_지난_리포트_다시_보기가_없다() throws Exception {
        LessonActivityCard exam = new LessonActivityCard(null, LessonActivityCard.EXAM, "모의고사",
                ActivityStatus.IN_PROGRESS, true, null, null, null, null, null, null,
                "/lessons/1/exam/attempts", null);
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.ok(
                new LessonPageData(1L, 1L, "수업 1", "목표", List.of(exam))));

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("모의고사 응시하기"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("지난 리포트 다시 보기")))));
    }

    @Test
    void 운영자_미리보기에서는_리포트_링크가_값이_있어도_숨긴다() throws Exception {
        given(lessonPageService.getPage(2L, 1L, 1L, null, true)).willReturn(LessonPageResult.ok(examPageData(true)));

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAsAdmin(2L))))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("지난 리포트 다시 보기"))));
    }

    @Test
    void 학습자_화면에는_미리보기_안내가_없다() throws Exception {
        LessonActivityCard concept = new LessonActivityCard(1L, LessonActivityCard.CONCEPT, "개념 1",
                ActivityStatus.IN_PROGRESS, true, "본문", null, List.of(), null, null, null, null, null);
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.ok(
                new LessonPageData(1L, 1L, "수업 1", "목표", List.of(concept))));

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("class=\"ls-preview-notice\"")),
                        org.hamcrest.Matchers.containsString("읽기 완료 · 퀴즈로"))));
    }

    @Test
    void 수강_안_한_코스면_코스_상세로_보낸다() throws Exception {
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.notEnrolled());

        mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1"));
    }

    @Test
    void 잠긴_수업이면_내_학습으로_토스트와_함께_보낸다() throws Exception {
        given(lessonPageService.getPage(1L, 1L, 1L, null, false)).willReturn(LessonPageResult.locked());

        MvcResult result = mockMvc.perform(get("/courses/1/lessons/1").with(authentication(loginAs(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/learning"))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("toast")).isEqualTo(LessonErrorCode.LESSON_LOCKED.getMessage());
    }

    @Test
    void 개념_완료가_잠겨_있으면_수업_화면으로_토스트와_함께_보낸다() throws Exception {
        given(lessonActivityService.completeConcept(1L, 1L, 1L, 10L)).willReturn(CompleteActivityResult.ofLocked());

        MvcResult result = mockMvc.perform(post("/courses/1/lessons/1/activities/10/complete")
                        .with(csrf())
                        .with(authentication(loginAs(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/lessons/1"))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("toast")).isEqualTo(LessonErrorCode.ACTIVITY_LOCKED.getMessage());
    }

    @Test
    void 개념_완료에_성공하면_바로_뒤_퀴즈_화면으로_보낸다() throws Exception {
        given(lessonActivityService.completeConcept(1L, 1L, 1L, 10L))
                .willReturn(CompleteActivityResult.redirectTo("/courses/1/lessons/1/quizzes/11"));

        mockMvc.perform(post("/courses/1/lessons/1/activities/10/complete")
                        .with(csrf())
                        .with(authentication(loginAs(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/lessons/1/quizzes/11"));
    }
}
