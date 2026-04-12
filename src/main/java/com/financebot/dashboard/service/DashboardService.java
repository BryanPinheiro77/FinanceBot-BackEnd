package com.financebot.dashboard.service;

import com.financebot.dashboard.dto.MonthlySummaryResponse;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        YearMonth referenceMonth = YearMonth.now();
        LocalDate startDate = referenceMonth.atDay(1);
        LocalDate endDate = referenceMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndTypeBetweenDates(
                user.getId(),
                TransactionType.INCOME,
                startDate,
                endDate
        );

        BigDecimal totalExpense = transactionRepository.sumAmountByUserAndTypeBetweenDates(
                user.getId(),
                TransactionType.EXPENSE,
                startDate,
                endDate
        );

        return new MonthlySummaryResponse(
                referenceMonth,
                startDate,
                endDate,
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense)
        );
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is invalid");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }
}