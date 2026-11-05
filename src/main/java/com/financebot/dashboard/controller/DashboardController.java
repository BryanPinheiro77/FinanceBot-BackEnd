package com.financebot.dashboard.controller;

import com.financebot.dashboard.dto.MonthlySummaryResponse;
import com.financebot.dashboard.service.DashboardService;
import com.financebot.user.service.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboard;
    private final AuthenticatedUserResolver users;

    @GetMapping("/monthly-summary")
    public MonthlySummaryResponse monthlySummary(@RequestParam String month, Authentication authentication) {
        YearMonth period;
        try {
            if (!month.matches("[0-9]{4}-(0[1-9]|1[0-2])") || month.startsWith("0000")) {
                throw new IllegalArgumentException("Informe o mês no formato AAAA-MM, entre 0001-01 e 9999-12.");
            }
            period = YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Informe um mês válido no formato AAAA-MM.");
        }
        return dashboard.monthlySummary(users.resolve(authentication).getId(), period);
    }
}
