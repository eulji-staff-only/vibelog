package com.vibelog.domain.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dto.CourseCard;
import com.vibelog.domain.course.model.dto.CourseListView;
import com.vibelog.domain.course.model.dto.TrackChip;
import com.vibelog.domain.course.model.dto.TrackCourseGroup;
import com.vibelog.domain.course.model.dto.TrackIntroView;
import com.vibelog.domain.course.model.service.CourseBrowseService;
import com.vibelog.global.exception.BusinessException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

/* 트랙 소개·코스 목록·코스 상세 화면이 데이터와 함께 오류 없이 그려지는지 확인한다 (#53, #97) */
@WebMvcTest(CoursePageController.class)
@WithMockUser   // @WebMvcTest 는 SecurityConfig 를 읽지 않고 기본 보안으로 모든 주소를 막는다 (CLAUDE.md 화면 선행 작업)
class CoursePageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseBrowseService courseBrowseService;

    private static final CourseCard CARD = new CourseCard(1L, 2L, "AI 작업 범위 통제하기", "한 줄 소개", "Controller", "프롬프트", 14, 4);

    @Test
    void 트랙_소개_화면을_연다() throws Exception {
        given(courseBrowseService.getTrackIntro(2L))
                .willReturn(new TrackIntroView(2L, "Controller", 2, "통제가 필요한 학습자", "트랙 설명", List.of(CARD)));

        mockMvc.perform(get("/tracks/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/track"))
                .andExpect(content().string(Matchers.containsString("2단계 트랙")))
                .andExpect(content().string(Matchers.containsString("AI 작업 범위 통제하기")))
                .andExpect(content().string(Matchers.containsString("수업 4개")))
                .andExpect(content().string(Matchers.containsString("첫 코스 보기")))
                .andExpect(content().string(Matchers.containsString("vl-footer")));
    }

    @Test
    void 없는_트랙은_404_오류_화면() throws Exception {
        given(courseBrowseService.getTrackIntro(999L)).willThrow(new BusinessException(CourseErrorCode.TRACK_NOT_FOUND));

        mockMvc.perform(get("/tracks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 코스_목록_화면을_추천_트랙과_함께_연다() throws Exception {
        TrackCourseGroup group = new TrackCourseGroup(2L, "Controller", List.of(CARD));
        given(courseBrowseService.getCourseList(any(), eq(2L)))
                .willReturn(new CourseListView(List.of(new TrackChip(2L, "Controller")), 2L, group, List.of(group), 1));

        mockMvc.perform(get("/courses").param("track", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/list"))
                .andExpect(content().string(Matchers.containsString("class=\"crs-recommend\"")))
                .andExpect(content().string(Matchers.containsString("AI 작업 범위 통제하기")))
                .andExpect(content().string(Matchers.containsString("조건 초기화")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("조건에 맞는 코스가 없습니다"))));
    }

    @Test
    void 결과가_없으면_안내_상자를_보여_준다() throws Exception {
        given(courseBrowseService.getCourseList(any(), any()))
                .willReturn(new CourseListView(List.of(), null, null, List.of(), 0));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("조건에 맞는 코스가 없습니다")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("class=\"crs-recommend\""))));
    }

    @Test
    void 코스_상세_화면을_연다() throws Exception {
        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("course/detail"))
                .andExpect(content().string(Matchers.containsString("수강 시작")))
                .andExpect(content().string(Matchers.containsString("vl-footer")));
    }
}
