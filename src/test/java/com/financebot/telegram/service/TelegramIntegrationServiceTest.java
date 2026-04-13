package com.financebot.telegram.service;

import com.financebot.account.repository.AccountRepository;
import com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.telegram.dto.request.InstallmentPurchaseCapacityRequest;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.service.TransactionService;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramIntegrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FinancialAnalysisService financialAnalysisService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TelegramAccountResolverService telegramAccountResolverService;

    @Mock
    private TelegramCategoryResolverService telegramCategoryResolverService;

    @InjectMocks
    private TelegramIntegrationService telegramIntegrationService;

    @Test
    @DisplayName("deve delegar analise de compra parcelada para o financial analysis service")
    void shouldDelegateInstallmentPurchaseCapacityAnalysis() {
        User user = new User();
        user.setId(1L);
        user.setTelegramId(123L);

        InstallmentPurchaseCapacityRequest request = new InstallmentPurchaseCapacityRequest(
                123L,
                new BigDecimal("2400"),
                12
        );

        InstallmentPurchaseCapacityResponse expectedResponse = new InstallmentPurchaseCapacityResponse(
                new BigDecimal("2400"),
                12,
                new BigDecimal("200.00"),
                "ALERTA",
                "observacao"
        );

        when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
        when(financialAnalysisService.analyzeInstallmentPurchaseCapacity(user, new BigDecimal("2400"), 12))
                .thenReturn(expectedResponse);

        InstallmentPurchaseCapacityResponse response =
                telegramIntegrationService.analyzeInstallmentPurchaseCapacity(request);

        assertThat(response).isEqualTo(expectedResponse);
        verify(financialAnalysisService).analyzeInstallmentPurchaseCapacity(user, new BigDecimal("2400"), 12);
    }

    @Test
    @DisplayName("deve retornar bad request quando valor total for invalido")
    void shouldThrowBadRequestWhenTotalAmountIsInvalid() {
        InstallmentPurchaseCapacityRequest request = new InstallmentPurchaseCapacityRequest(
                123L,
                BigDecimal.ZERO,
                12
        );

        assertThatThrownBy(() -> telegramIntegrationService.analyzeInstallmentPurchaseCapacity(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    @DisplayName("deve retornar bad request quando quantidade de parcelas for invalida")
    void shouldThrowBadRequestWhenTotalInstallmentsIsInvalid() {
        InstallmentPurchaseCapacityRequest request = new InstallmentPurchaseCapacityRequest(
                123L,
                new BigDecimal("2400"),
                1
        );

        assertThatThrownBy(() -> telegramIntegrationService.analyzeInstallmentPurchaseCapacity(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    @DisplayName("deve lancar erro quando usuario do telegram nao existir")
    void shouldThrowWhenTelegramUserDoesNotExist() {
        InstallmentPurchaseCapacityRequest request = new InstallmentPurchaseCapacityRequest(
                123L,
                new BigDecimal("2400"),
                12
        );

        when(userRepository.findByTelegramId(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> telegramIntegrationService.analyzeInstallmentPurchaseCapacity(request))
                .isInstanceOf(TelegramUserNotFoundException.class);
    }
}
