package com.vibelog.domain.admin.controller;

import com.vibelog.domain.admin.model.dto.AdminDashboardResponse;
import com.vibelog.domain.admin.model.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/*
 * [AdminDashboardController]
 * 역할 : 운영자 기본 대시보드 화면 (A-01, API-ADM-01, FR-ADM-001, FR-ADM-002). 운영자 로그인 도착지다.
 * 책임 : - 기간 필터 from·to 는 쿼리 파라미터로 받는다. 비우면 전체 기간이다.
 *        - 화면만 그린다. 운영자인지는 SecurityConfig(/admin/** hasRole("ADMIN"))가 이미 막는다.
 */
@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/admin")
    public String dashboard(@RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             Model model) {
        AdminDashboardResponse dashboard = adminDashboardService.getDashboard(from, to);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/dashboard";
    }
}
