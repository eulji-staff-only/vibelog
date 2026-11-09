package com.vibelog.domain.exam.controller;

import com.vibelog.domain.exam.model.dto.AdminExamQuestionItem;
import com.vibelog.domain.exam.model.dto.ExamQuestionForm;
import com.vibelog.domain.exam.model.service.ExamAdminService;
import com.vibelog.domain.lesson.controller.LessonAdminController;
import com.vibelog.domain.lesson.model.dto.AdminActivityItem;
import com.vibelog.domain.lesson.model.dto.LessonEditView;
import com.vibelog.domain.lesson.model.dto.LessonFormRequest;
import com.vibelog.domain.lesson.model.dto.LessonPublishStatus;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.service.LessonAdminService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 모의고사 문항 추가·수정·사용 여부 요청(API-ADM-29~31)이 A-06 모의고사 탭으로 돌아가고,
 * 입력이 틀리면 칸별 메시지를 flash 로 넘겨 A-06 이 그 모달을 열린 채로 다시 그리는지 확인한다 (DB 없이 도는 슬라이스 테스트).
 * A-06 화면은 lesson 영역 Controller 가 그리므로 함께 띄워 exam 조각이 오류 없이 그려지는지도 잡는다.
 * @WebMvcTest 는 SecurityConfig 를 읽지 않아 운영자로 로그인한 것처럼 둔다.
 */
@WebMvcTest({ExamAdminController.class, LessonAdminController.class})
@WithMockUser(roles = "ADMIN")
class ExamAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExamAdminService examAdminService;

    @MockitoBean
    LessonAdminService lessonAdminService;

    @Test
    void 문항을_추가하면_모의고사_탭으로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/lessons/5/exam-questions").with(csrf())
                        .param("content", "문항").param("explanation", "해설").param("conceptActivityId", "11")
                        .param("choices", "가", "나", "다", "라").param("correctIndex", "1"))
                .andExpect(redirectedUrl("/admin/lessons/5?tab=exam"))
                .andExpect(flash().attributeCount(0));

        verify(examAdminService).create(eq(5L), any(ExamQuestionForm.class));
    }

    @Test
    void 연관_개념과_보기가_비면_칸별_메시지를_넘기고_저장하지_않는다() throws Exception {
        mockMvc.perform(post("/admin/lessons/5/exam-questions").with(csrf())
                        .param("content", "문항").param("explanation", "해설").param("conceptActivityId", "")
                        .param("choices", "가", "", "다", "라").param("correctIndex", "1"))
                .andExpect(redirectedUrl("/admin/lessons/5?tab=exam"))
                .andExpect(flash().attribute("examQuestionTarget", "new"))
                .andExpect(flash().attribute("examQuestionErrors", Map.of(
                        "conceptActivityId", "연관 개념을 골라야 저장할 수 있습니다.",
                        "choices", "보기 4개를 모두 입력해 주세요.")));

        verify(examAdminService, never()).create(any(), any());
    }

    @Test
    void 수정이_틀리면_그_문항의_수업으로_돌아가_그_모달을_가리킨다() throws Exception {
        given(examAdminService.getLessonId(21L)).willReturn(5L);

        mockMvc.perform(post("/admin/exam-questions/21/edit").with(csrf())
                        .param("content", "").param("explanation", "해설").param("conceptActivityId", "11")
                        .param("choices", "가", "나", "다", "라").param("correctIndex", "0"))
                .andExpect(redirectedUrl("/admin/lessons/5?tab=exam"))
                .andExpect(flash().attribute("examQuestionTarget", "21"));

        verify(examAdminService, never()).edit(any(), any());
    }

    @Test
    void 사용_여부를_바꾸면_모의고사_탭으로_돌아간다() throws Exception {
        given(examAdminService.changeActive(21L, false)).willReturn(5L);

        mockMvc.perform(post("/admin/exam-questions/21/active").with(csrf()).param("active", "false"))
                .andExpect(redirectedUrl("/admin/lessons/5?tab=exam"));
    }

    @Test
    void A06_모의고사_탭은_문항_목록과_수정_모달을_저장된_값으로_그린다() throws Exception {
        givenEditView();

        mockMvc.perform(get("/admin/lessons/5").param("tab", "exam"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("저장된 문항")))
                .andExpect(content().string(Matchers.containsString("/admin/exam-questions/21/edit")))
                .andExpect(content().string(Matchers.containsString("value=\"저장된 보기2\"")))
                .andExpect(content().string(Matchers.containsString("/admin/lessons/5/exam-questions")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("연관 개념을 골라야"))));
    }

    @Test
    void 입력_오류로_돌아오면_추가_모달을_열고_보낸_값과_칸별_메시지를_다시_그린다() throws Exception {
        givenEditView();
        ExamQuestionForm sent = new ExamQuestionForm();
        sent.setContent("보냈던 문항");
        sent.setChoices(List.of("보냈던 보기1", "", "", ""));

        mockMvc.perform(get("/admin/lessons/5").param("tab", "exam")
                        .flashAttr("examQuestionForm", sent)
                        .flashAttr("examQuestionErrors", Map.of("conceptActivityId", "연관 개념을 골라야 저장할 수 있습니다."))
                        .flashAttr("examQuestionTarget", "new"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("보냈던 문항")))
                .andExpect(content().string(Matchers.containsString("value=\"보냈던 보기1\"")))
                .andExpect(content().string(Matchers.containsString("연관 개념을 골라야 저장할 수 있습니다.")))
                // 추가 모달만 hidden 없이 열린 채로 그린다
                .andExpect(content().string(Matchers.containsString("id=\"exam-question-add-modal\" role=\"dialog\"")))
                // 열린 모달 제목에만 autofocus (키보드 초점이 오류 난 모달로 간다)
                .andExpect(content().string(Matchers.containsString("id=\"exam-question-add-modal-title\" autofocus=\"autofocus\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("id=\"exam-question-edit-modal-21-title\" autofocus"))));
    }

    private void givenEditView() {
        AdminExamQuestionItem question = new AdminExamQuestionItem(21L, "저장된 문항", "저장된 해설", 11L, 1, true, List.of(
                new AdminExamQuestionItem.Choice("저장된 보기1", false, 1),
                new AdminExamQuestionItem.Choice("저장된 보기2", true, 2)));
        AdminActivityItem concept = new AdminActivityItem(11L, ActivityType.CONCEPT, "개념 하나", "본문", null, 10,
                List.of(), 1, true);
        given(lessonAdminService.getEditView(5L, "exam")).willReturn(new LessonEditView(5L, 3L, "코스", "수업", false,
                "exam", List.of(concept), List.of(), List.of(question), new LessonPublishStatus(true, List.of())));
        given(lessonAdminService.getForm(5L)).willReturn(new LessonFormRequest());
    }
}
