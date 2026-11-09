package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.model.dto.ReportRequest;
import com.vibelog.domain.message.model.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/*
 * [MessageReportApiController]
 * 역할 : 신고하기 (API-RPT-01, M-11). 성공하면 204 이고, 화면이 모달을 닫고 "신고가 접수되었습니다" 토스트를 띄운다.
 * 책임 : 회원만 (SecurityConfig). 실패는 JSON {code, message} (GlobalExceptionHandler).
 */
@RestController
@RequiredArgsConstructor
public class MessageReportApiController {

    private final ReportService reportService;

    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@Valid @RequestBody ReportRequest request, @AuthenticationPrincipal LoginMember loginMember) {
        reportService.report(loginMember.getMemberId(), request);
    }
}
