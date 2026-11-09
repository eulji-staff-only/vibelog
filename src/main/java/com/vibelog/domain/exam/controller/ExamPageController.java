package com.vibelog.domain.exam.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.dto.ExamStartResult;
import com.vibelog.domain.exam.model.dto.ExamSubmitRequest;
import com.vibelog.domain.exam.model.dto.ExamSubmitResult;
import com.vibelog.domain.exam.model.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [ExamPageController]
 * 역할 : 모의고사 응시 시작(API-EXM-01)·화면(API-EXM-02)·제출(API-EXM-04)을 받는다.
 * 책임 : - 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - EXAM_LOCKED·REPORT_NOT_VIEWED·ATTEMPT_SUBMITTED 는 "다른 화면 + 토스트"(_template.md 7-1 ③)라서
 *          Service 가 돌려준 결과를 보고 여기서 redirect 한다 (LessonPageController 와 같은 방식).
 */
@Controller
@RequiredArgsConstructor
public class ExamPageController {

    private final ExamService examService;

    /* API-EXM-01 응시 시작·이어 풀기 */
    @PostMapping("/lessons/{lessonId}/exam/attempts")
    public String start(@PathVariable Long lessonId,
                        @AuthenticationPrincipal LoginMember loginMember,
                        RedirectAttributes redirectAttributes) {
        ExamStartResult result = examService.start(loginMember.getMemberId(), lessonId);
        return switch (result.status()) {
            case OK -> "redirect:/lessons/" + lessonId + "/exam";
            case LOCKED -> {
                redirectAttributes.addFlashAttribute("toast", ExamErrorCode.EXAM_LOCKED.getMessage());
                yield "redirect:/my/learning";
            }
            case REPORT_NOT_VIEWED -> {
                redirectAttributes.addFlashAttribute("toast", ExamErrorCode.REPORT_NOT_VIEWED.getMessage());
                yield reportUrl(lessonId, result.attemptNo());
            }
        };
    }

    /* U-15 모의고사 (API-EXM-02). 진행 중인 응시가 없으면 수업 화면의 모의고사 항목으로 */
    @GetMapping("/lessons/{lessonId}/exam")
    public String exam(@PathVariable Long lessonId,
                       @AuthenticationPrincipal LoginMember loginMember,
                       Model model) {
        return examService.getPage(loginMember.getMemberId(), lessonId)
                .map(page -> {
                    model.addAttribute("page", page);
                    return "exam/exam";
                })
                .orElseGet(() -> "redirect:/courses/" + examService.getCourseId(lessonId)
                        + "/lessons/" + lessonId + "?activity=exam");
    }

    /* API-EXM-04 제출·채점 → 오답 분석 리포트 */
    @PostMapping("/lessons/{lessonId}/exam/submit")
    public String submit(@PathVariable Long lessonId,
                         @Valid @ModelAttribute ExamSubmitRequest request,
                         @AuthenticationPrincipal LoginMember loginMember,
                         RedirectAttributes redirectAttributes) {
        ExamSubmitResult result = examService.submit(loginMember.getMemberId(), lessonId,
                request.getExamAttemptId(), request.getAnswers());
        if (result.alreadySubmitted()) {
            redirectAttributes.addFlashAttribute("toast", ExamErrorCode.ATTEMPT_SUBMITTED.getMessage());
        }
        return reportUrl(lessonId, result.attemptNo());
    }

    private String reportUrl(Long lessonId, int attemptNo) {
        return "redirect:/lessons/" + lessonId + "/exam/report?attempt=" + attemptNo;
    }
}
