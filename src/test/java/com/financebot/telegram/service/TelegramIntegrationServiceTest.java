package com.financebot.telegram.service;

import com.financebot.account.domain.Account;
import com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.category.domain.Category;
import com.financebot.telegram.dto.request.CreateTransactionFromTelegramRequest;
import com.financebot.telegram.dto.request.InstallmentPurchaseCapacityRequest;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.transaction.application.command.CreateTransactionCommand;
import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
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
    private CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;

    @Mock
    private CreateTransactionUseCase createTransactionUseCase;

    @Mock
    private TelegramAccountResolverService telegramAccountResolverService;

    @Mock
    private TelegramCategoryResolverService telegramCategoryResolverService;

    @InjectMocks
    private TelegramIntegrationService telegramIntegrationService;

    @Nested
    @DisplayName("createTransactionFromTelegram")
    class CreateTransactionFromTelegramTests {

        @Test
        @DisplayName("deve delegar criacao de transacao comum para o use case")
        void shouldDelegateCommonTransactionCreationToUseCase() {
            User user = new User();
            user.setId(1L);
            user.setTelegramId(123L);

            Account account = new Account();
            account.setId(10L);
            account.setName("Carteira");

            Category category = new Category();
            category.setId(20L);
            category.setName("Alimentação");

            CreateTransactionFromTelegramRequest request = new CreateTransactionFromTelegramRequest(
                    123L,
                    "EXPENSE",
                    new BigDecimal("50.00"),
                    "lanche",
                    LocalDate.of(2026, 5, 30),
                    "Alimentação",
                    "Carteira"
            );

            when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
            when(telegramAccountResolverService.resolve(user, "Carteira")).thenReturn(account);
            when(telegramCategoryResolverService.resolveExplicitCategory(
                    user,
                    TransactionType.EXPENSE,
                    "Alimentação"
            )).thenReturn(category);

            telegramIntegrationService.createTransactionFromTelegram(request);

            verify(createTransactionUseCase).execute(
                    argThat(command ->
                            command.amount().compareTo(new BigDecimal("50.00")) == 0
                                    && command.description().equals("lanche")
                                    && command.date().equals(LocalDate.of(2026, 5, 30))
                                    && command.type() == TransactionType.EXPENSE
                                    && command.sourceType() == SourceType.BOT_TEXT
                                    && command.accountId().equals(10L)
                                    && command.categoryId().equals(20L)
                                    && command.user().equals(user)
                    )
            );

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve usar categoria automatica quando categoria nao for informada")
        void shouldUseAutomaticCategoryWhenCategoryNameIsNotProvided() {
            User user = new User();
            user.setId(1L);
            user.setTelegramId(123L);

            Account account = new Account();
            account.setId(10L);
            account.setName("Carteira");

            Category category = new Category();
            category.setId(20L);
            category.setName("Alimentação");

            CreateTransactionFromTelegramRequest request = new CreateTransactionFromTelegramRequest(
                    123L,
                    "EXPENSE",
                    new BigDecimal("50.00"),
                    "lanche",
                    LocalDate.of(2026, 5, 30),
                    null,
                    "Carteira"
            );

            when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
            when(telegramAccountResolverService.resolve(user, "Carteira")).thenReturn(account);
            when(telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    "lanche"
            )).thenReturn(category);

            telegramIntegrationService.createTransactionFromTelegram(request);

            verify(telegramCategoryResolverService).resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    "lanche"
            );

            verify(createTransactionUseCase).execute(
                    any(CreateTransactionCommand.class)
            );

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve usar categoria automatica quando categoria vier em branco")
        void shouldUseAutomaticCategoryWhenCategoryNameIsBlank() {
            User user = new User();
            user.setId(1L);
            user.setTelegramId(123L);

            Account account = new Account();
            account.setId(10L);
            account.setName("Carteira");

            Category category = new Category();
            category.setId(20L);
            category.setName("Alimentação");

            CreateTransactionFromTelegramRequest request = new CreateTransactionFromTelegramRequest(
                    123L,
                    "EXPENSE",
                    new BigDecimal("50.00"),
                    "lanche",
                    LocalDate.of(2026, 5, 30),
                    "   ",
                    "Carteira"
            );

            when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
            when(telegramAccountResolverService.resolve(user, "Carteira")).thenReturn(account);
            when(telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    "lanche"
            )).thenReturn(category);

            telegramIntegrationService.createTransactionFromTelegram(request);

            verify(telegramCategoryResolverService).resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    "lanche"
            );

            verify(createTransactionUseCase).execute(
                    any(CreateTransactionCommand.class)
            );

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar erro quando usuario do telegram nao existir")
        void shouldThrowWhenTelegramUserDoesNotExistOnCreateTransaction() {
            CreateTransactionFromTelegramRequest request = new CreateTransactionFromTelegramRequest(
                    123L,
                    "EXPENSE",
                    new BigDecimal("50.00"),
                    "lanche",
                    LocalDate.of(2026, 5, 30),
                    "Alimentação",
                    "Carteira"
            );

            when(userRepository.findByTelegramId(123L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> telegramIntegrationService.createTransactionFromTelegram(request))
                    .isInstanceOf(TelegramUserNotFoundException.class);

            verify(createTransactionUseCase, never())
                    .execute(any(CreateTransactionCommand.class));

            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("analyzeInstallmentPurchaseCapacity")
    class AnalyzeInstallmentPurchaseCapacityTests {

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

            verify(financialAnalysisService)
                    .analyzeInstallmentPurchaseCapacity(user, new BigDecimal("2400"), 12);
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

    @Nested
    @DisplayName("stripInstallmentSuffix")
    class StripInstallmentSuffixTests {

        @Test
        @DisplayName("deve remover sufixo de parcela simples")
        void shouldRemoveSimpleInstallmentSuffix() throws Exception {
            String result = invokeStripInstallmentSuffix("Netflix - 2/10");

            assertThat(result).isEqualTo("Netflix");
        }

        @Test
        @DisplayName("deve remover sufixo de parcela com espacos")
        void shouldRemoveInstallmentSuffixWithSpaces() throws Exception {
            String result = invokeStripInstallmentSuffix("Cartao mercado   -   12/12   ");

            assertThat(result).isEqualTo("Cartao mercado");
        }

        @Test
        @DisplayName("deve remover apenas o ultimo sufixo quando descricao tiver hifens")
        void shouldRemoveOnlyLastInstallmentSuffixWhenDescriptionHasHyphens() throws Exception {
            String result = invokeStripInstallmentSuffix("Compra - mercado - 3/6");

            assertThat(result).isEqualTo("Compra - mercado");
        }

        @Test
        @DisplayName("deve manter descricao sem sufixo de parcela")
        void shouldKeepDescriptionWithoutInstallmentSuffix() throws Exception {
            String result = invokeStripInstallmentSuffix("Netflix");

            assertThat(result).isEqualTo("Netflix");
        }

        @Test
        @DisplayName("deve manter descricao quando texto apos hifen nao for parcela")
        void shouldKeepDescriptionWhenSuffixIsNotInstallmentPattern() throws Exception {
            String result = invokeStripInstallmentSuffix("Compra - mercado");

            assertThat(result).isEqualTo("Compra - mercado");
        }

        @Test
        @DisplayName("deve manter descricao quando sufixo tiver formato invalido")
        void shouldKeepDescriptionWhenInstallmentSuffixIsInvalid() throws Exception {
            String result = invokeStripInstallmentSuffix("Compra - 2/a");

            assertThat(result).isEqualTo("Compra - 2/a");
        }

        @Test
        @DisplayName("deve retornar string vazia quando descricao for vazia")
        void shouldReturnEmptyStringWhenDescriptionIsEmpty() throws Exception {
            String result = invokeStripInstallmentSuffix("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve retornar null quando descricao for null")
        void shouldReturnNullWhenDescriptionIsNull() throws Exception {
            String result = invokeStripInstallmentSuffix(null);

            assertThat(result).isNull();
        }

        private String invokeStripInstallmentSuffix(String description) throws Exception {
            Method method = TelegramIntegrationService.class
                    .getDeclaredMethod("stripInstallmentSuffix", String.class);

            method.setAccessible(true);

            return (String) method.invoke(telegramIntegrationService, description);
        }
    }
}