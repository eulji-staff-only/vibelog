package com.vibelog.domain.exam.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.service.ExamReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/*
 * [ExamReportPageController]
 * 역할 : 오답 분석 리포트 화면(U-16, API-EXM-05)을 연다.
 * 책임 : - 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 수강을 포기했거나 수강 중이 아니면 COURSE_NOT_ENROLLED: 코스 상세 + toast (#439). 리포트 기록·수업 완료는 하지 않는다.
 *        - 제출한 응시가 없으면 EXAM_NOT_SUBMITTED: 모의고사 화면 + toast (_template.md 7-1 ③).
 *        - 이 GET 은 리포트 연 일시를 기록한다 (명세가 정한 예외, _template.md 0-1).
 */
@Controller
@RequiredArgsConstructor
public class ExamReportPageController {

    private final ExamReportService examReportService;

    /* U-16 오답 분석 리포트. attempt(회차)가 없으면 최근 제출한 응시 */
    @GetMapping("/lessons/{lessonId}/exam/report")
    public String report(@PathVariable Long lessonId,
                         @RequestParam(required = false) Integer attempt,
                         @AuthenticationPrincipal LoginMember loginMember,
                         Model model, RedirectAttributes redirectAttributes) {
        Optional<Long> notLearningCourseId = examReportService.findCourseIdIfNotLearning(loginMember.getMemberId(), lessonId);
        if (notLearningCourseId.isPresent()) {
            redirectAttributes.addFlashAttribute("toast", ExamErrorCode.COURSE_NOT_ENROLLED.getMessage());
            return "redirect:/courses/" + notLearningCourseId.get();
        }
        return examReportService.getReport(loginMember.getMemberId(), lessonId, attempt)
                .map(report -> {
                    model.addAttribute("report", report);
                    return "exam/report";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("toast", ExamErrorCode.EXAM_NOT_SUBMITTED.getMessage());
                    return "redirect:/lessons/" + lessonId + "/exam";
                });
    }
}
