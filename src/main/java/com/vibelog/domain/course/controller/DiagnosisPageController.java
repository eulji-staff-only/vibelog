package com.vibelog.domain.course.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [DiagnosisPageController]
 * 역할 : 진단 설문·진단 결과 화면(GET)을 연다.
 * 책임 : - 화면 흐름 설계서의 화면 URL 로 화면 이름만 돌려준다. (화면 선행 작업, #52)
 *        - 문항·보기 데이터, 진단 완료·건너뛰기 저장, 추천 트랙 계산(PL-DGN-001·002)은 ③ 단계 기능 이슈(#99)에서 API 명세대로 붙인다.
 */
@Controller
public class DiagnosisPageController {

    /* U-07 진단 설문 (M-01 건너뛰기 확인 모달 포함) */
    @GetMapping("/diagnosis")
    public String diagnosis() {
        return "course/diagnosis";
    }

    /* U-08 진단 결과 */
    @GetMapping("/diagnosis/result")
    public String diagnosisResult() {
        return "course/diagnosis-result";
    }
}
