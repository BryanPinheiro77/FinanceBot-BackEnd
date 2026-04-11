package com.financebot.telegrambot.dto;

import java.time.LocalDate;

public record ParsedDateRange(
        LocalDate startDate,
        LocalDate endDate
) {
}