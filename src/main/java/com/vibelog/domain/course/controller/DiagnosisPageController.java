package com.vibelog.domain.course.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.course.model.dto.DiagnosisResultView;
import com.vibelog.domain.course.model.service.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/*
 * [DiagnosisPageController]
 * 역할 : 진단 설문·제출·건너뛰기·결과 (U-07, M-01, U-08, API-DGN-01~04, #99 / 진단 다시 하기 FR-DGN-005, #188).
 * 책임 : - 회원만 쓴다 (SecurityConfig). 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 추천 이유(고른 보기 내용)는 저장하지 않고 제출 직후 flash("reasons")로만 결과 화면에 넘긴다 (PL-DGN-001).
 *          저장한 결과 ID(flash "resultId")도 함께 넘겨, 결과 화면은 그 결과일 때만 이유를 보여준다.
 *        - 진단 기록이 있으면(다시 하기) 설문 화면이 건너뛰기 안내를 바꿔 보여준다 (retake).
 */
@Controller
@RequiredArgsConstructor
public class DiagnosisPageController {

    /* 제출한 답이 지금 문항과 맞지 않을 때 안내 (API-DGN-02 P2, course.md "이 명세에서 정한 것" 8) */
    static final String SURVEY_CHANGED = "진단 문항이 바뀌었습니다. 다시 진단해 주세요.";

    private final DiagnosisService diagnosisService;

    /* U-07 진단 설문 (API-DGN-01, M-01 건너뛰기 확인 모달 포함). 진단 기록이 있으면 다시 하기 (API-DGN-01 P2) */
    @GetMapping("/diagnosis")
    public String diagnosis(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("survey", diagnosisService.getSurvey());
        model.addAttribute("retake", diagnosisService.hasRecord(loginMember.getMemberId()));
        return "course/diagnosis";
    }

    /* API-DGN-02 진단 제출 → 결과 화면 */
    @PostMapping("/diagnosis")
    public String submit(@RequestParam(required = false) List<Long> diagnosisOptionIds,
                         @AuthenticationPrincipal LoginMember loginMember,
                         RedirectAttributes redirectAttributes) {
        Optional<DiagnosisService.SubmitResult> result = diagnosisService.submit(loginMember.getMemberId(), diagnosisOptionIds);
        if (result.isEmpty()) {
            // INVALID_INPUT: 오류 화면 대신 설문으로 돌아가 안내한다 (API-DGN-02 P2, ③). 다시 열린 설문은 지금 문항으로 그린다
            redirectAttributes.addFlashAttribute("toast", SURVEY_CHANGED);
            return "redirect:/diagnosis";
        }
        redirectAttributes.addFlashAttribute("reasons", result.get().reasons());
        redirectAttributes.addFlashAttribute("resultId", result.get().diagnosisResultId());
        return "redirect:/diagnosis/result";
    }

    /* API-DGN-03 진단 건너뛰기 (M-01 확인 후) → 코스 목록 */
    @PostMapping("/diagnosis/skip")
    public String skip(@AuthenticationPrincipal LoginMember loginMember) {
        diagnosisService.skip(loginMember.getMemberId());
        return "redirect:/courses";
    }

    /* U-08 진단 결과 (API-DGN-04). 결과가 없거나 건너뛰었으면 설문으로 보낸다 */
    @GetMapping("/diagnosis/result")
    @SuppressWarnings("unchecked")
    public String diagnosisResult(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        // 제출 직후에만 flash 로 들어온다. 나중에 다시 열면 없다 (PL-DGN-001)
        List<String> reasons = (List<String>) model.getAttribute("reasons");
        Long resultId = (Long) model.getAttribute("resultId");
        Optional<DiagnosisResultView> result = diagnosisService.getResult(loginMember.getMemberId(), reasons, resultId);
        if (result.isEmpty()) {
            return "redirect:/diagnosis";
        }
        model.addAttribute("result", result.get());
        return "course/diagnosis-result";
    }
}
