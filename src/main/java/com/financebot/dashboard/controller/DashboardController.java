package com.financebot.dashboard.controller;

import com.financebot.dashboard.dto.MonthlySummaryResponse;
import com.financebot.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public MonthlySummaryResponse getMonthlySummary(Authentication authentication) {
        return dashboardService.getMonthlySummary(authentication);
    }
}