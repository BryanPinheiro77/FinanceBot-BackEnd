package com.financebot.telegram.service;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.telegram.dto.request.*;
import com.financebot.telegram.dto.response.*;
import com.financebot.telegram.application.command.CreateTelegramExistingInstallmentCommand;
import com.financebot.telegram.application.command.CreateTelegramInstallmentCommand;
import com.financebot.telegram.application.command.CreateTelegramTransactionCommand;
import com.financebot.telegram.application.usecase.TelegramTransactionApplicationUseCase;
import com.financebot.telegram.application.usecase.TelegramFinancialQueryApplicationUseCase;
import com.financebot.telegram.application.usecase.TelegramUserApplicationUseCase;
import com.financebot.transaction.application.usecase.CreateExistingInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.user.dto.response.TelegramUserProfileResponse;
import com.financebot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelegramIntegrationService {

    private final UserRepository userRepository;
    private final FinancialAnalysisService financialAnalysisService;
    private final TransactionRepository transactionRepository;
    private final CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;
    private final TelegramAccountResolverService telegramAccountResolverService;
    private final TelegramCategoryResolverService telegramCategoryResolverService;
    private final CreateTransactionUseCase createTransactionUseCase;
    private final CreateExistingInstallmentTransactionUseCase createExistingInstallmentTransactionUseCase;
    private final TelegramTransactionApplicationUseCase telegramTransactionApplicationUseCase;
    private final TelegramFinancialQueryApplicationUseCase telegramFinancialQueryApplicationUseCase;
    private final TelegramUserApplicationUseCase telegramUserApplicationUseCase;

    @Transactional(readOnly = true)
    public TelegramUserProfileResponse getMe(Long telegramId) {
        return userUseCase().getProfile(telegramId);
    }

    @Transactional
    public TelegramUserProfileResponse updateMonthlyBaseIncome(
            Long telegramId,
            UpdateMonthlyBaseIncomeRequest request
    ) {
        return userUseCase().updateMonthlyIncome(telegramId, request.monthlyBaseIncome());
    }

    @Transactional
    public void disconnectTelegram(Long telegramId) {
        userUseCase().disconnect(telegramId);
    }

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse getFinancialAnalysis(Long telegramId) {
        return queryUseCase().financialAnalysis(telegramId);
    }

    private TelegramTransactionApplicationUseCase transactionUseCase() {
        return telegramTransactionApplicationUseCase != null
                ? telegramTransactionApplicationUseCase
                : new TelegramTransactionApplicationUseCase(
                        userRepository,
                        telegramAccountResolverService,
                        telegramCategoryResolverService,
                        createTransactionUseCase,
                        createInstallmentTransactionUseCase,
                        createExistingInstallmentTransactionUseCase
                );
    }

    private TelegramUserApplicationUseCase userUseCase() {
        return telegramUserApplicationUseCase != null
                ? telegramUserApplicationUseCase
                : new TelegramUserApplicationUseCase(userRepository, telegramAccountResolverService);
    }

    private TelegramFinancialQueryApplicationUseCase queryUseCase() {
        return telegramFinancialQueryApplicationUseCase != null
                ? telegramFinancialQueryApplicationUseCase
                : new TelegramFinancialQueryApplicationUseCase(
                        userRepository, transactionRepository, financialAnalysisService
                );
    }

    @Transactional(readOnly = true)
    public MonthlyAmountSummaryResponse getCurrentMonthExpenseSummary(Long telegramId) {
        return queryUseCase().currentMonthSummary(telegramId, TransactionType.EXPENSE);
    }

    @Transactional(readOnly = true)
    public MonthlyAmountSummaryResponse getCurrentMonthIncomeSummary(Long telegramId) {
        return queryUseCase().currentMonthSummary(telegramId, TransactionType.INCOME);
    }

    @Transactional
    public void createTransactionFromTelegram(CreateTransactionFromTelegramRequest request) {
        transactionUseCase().create(new CreateTelegramTransactionCommand(
                request.telegramId(),
                request.amount(),
                request.description(),
                request.date(),
                TransactionType.valueOf(request.type()),
                request.categoryName(),
                request.accountName()
        ));
    }

    @Transactional
    public void createInstallmentTransactionFromTelegram(CreateInstallmentTransactionFromTelegramRequest request) {
        transactionUseCase().createInstallment(new CreateTelegramInstallmentCommand(
                request.telegramId(),
                request.totalAmount(),
                request.description(),
                request.firstInstallmentDate(),
                request.accountName(),
                request.categoryName(),
                request.totalInstallments()
        ));
    }

    @Transactional
    public void createExistingInstallmentTransactionFromTelegram(CreateExistingInstallmentTransactionFromTelegramRequest request) {
        transactionUseCase().createExistingInstallment(new CreateTelegramExistingInstallmentCommand(
                request.telegramId(),
                request.totalAmount(),
                request.monthlyAmount(),
                request.description(),
                request.firstRemainingInstallmentDate(),
                request.accountName(),
                request.categoryName(),
                request.totalInstallments(),
                request.firstRemainingInstallmentNumber()
        ));
    }

    @Transactional(readOnly = true)
    public TelegramTransactionSummaryResponse getTransactionSummary(TelegramTransactionSummaryRequest request) {
        return queryUseCase().transactionSummary(
                request.telegramId(), TransactionType.valueOf(request.type()), request.type(),
                request.categoryName(), request.accountName(), request.startDate(), request.endDate()
        );
    }

    public TelegramDefaultAccountResponse getDefaultAccount(Long telegramId) {
        return userUseCase().getDefaultAccount(telegramId);
    }

    @Transactional(readOnly = true)
    public TelegramInstallmentCountResponse getInstallmentCount(TelegramInstallmentCountRequest request) {
        return queryUseCase().installmentCount(
                request.telegramId(), request.startDate(), request.endDate()
        );
    }

    @Transactional(readOnly = true)
    public InstallmentPurchaseCapacityResponse analyzeInstallmentPurchaseCapacity(
            InstallmentPurchaseCapacityRequest request
    ) {
        return queryUseCase().installmentCapacity(
                request.telegramId(), request.totalAmount(), request.totalInstallments()
        );
    }

    @Transactional(readOnly = true)
    public TelegramActiveInstallmentsResponse getActiveInstallments(Long telegramId) {
        return queryUseCase().activeInstallments(telegramId);
    }

    @Transactional(readOnly = true)
    public TelegramActiveInstallmentSummaryResponse getActiveInstallmentSummary(Long telegramId, String query) {
        return queryUseCase().activeInstallmentSummary(telegramId, query);
    }

    /**
     * Compatibility bridge for existing reflective tests and integrations.
     * The active-installment flow itself is implemented by the query use case.
     */
    private String stripInstallmentSuffix(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }

        String trimmedDescription = description.trim();
        int lastDashIndex = trimmedDescription.lastIndexOf('-');
        if (lastDashIndex == -1) {
            return trimmedDescription;
        }

        String possibleSuffix = trimmedDescription.substring(lastDashIndex + 1).trim();
        String[] parts = possibleSuffix.split("/");
        if (parts.length != 2 || !isPositiveInteger(parts[0]) || !isPositiveInteger(parts[1])) {
            return trimmedDescription;
        }

        return trimmedDescription.substring(0, lastDashIndex).trim();
    }

    private boolean isPositiveInteger(String value) {
        return value != null && !value.isBlank() && value.chars().allMatch(Character::isDigit);
    }

}
