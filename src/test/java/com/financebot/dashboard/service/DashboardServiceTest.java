package com.financebot.dashboard.service;

import com.financebot.dashboard.dto.MonthlySummaryResponse;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("deve retornar resumo mensal com receitas, despesas e saldo")
    void shouldReturnMonthlySummary() {
        User user = new User();
        user.setId(1L);
        user.setEmail("bryan@email.com");

        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByUserAndTypeBetweenDates(1L, TransactionType.INCOME, startDate, endDate))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumAmountByUserAndTypeBetweenDates(1L, TransactionType.EXPENSE, startDate, endDate))
                .thenReturn(new BigDecimal("1850.35"));

        MonthlySummaryResponse response = dashboardService.getMonthlySummary(authentication);

        assertThat(response.referenceMonth()).isEqualTo(currentMonth);
        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.endDate()).isEqualTo(endDate);
        assertThat(response.totalIncome()).isEqualByComparingTo("5000.00");
        assertThat(response.totalExpense()).isEqualByComparingTo("1850.35");
        assertThat(response.balance()).isEqualByComparingTo("3149.65");

        verify(transactionRepository).sumAmountByUserAndTypeBetweenDates(1L, TransactionType.INCOME, startDate, endDate);
        verify(transactionRepository).sumAmountByUserAndTypeBetweenDates(1L, TransactionType.EXPENSE, startDate, endDate);
    }

    @Test
    @DisplayName("deve lançar erro quando usuário autenticado for inválido")
    void shouldThrowWhenAuthenticationIsInvalid() {
        assertThatThrownBy(() -> dashboardService.getMonthlySummary(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authenticated user is invalid");
    }

    @Test
    @DisplayName("deve lançar erro quando usuário autenticado não for encontrado")
    void shouldThrowWhenAuthenticatedUserIsNotFound() {
        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getMonthlySummary(authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Authenticated user not found");
    }
}