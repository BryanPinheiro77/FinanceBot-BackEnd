package com.financebot.analysis.service;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.category.domain.Category;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.dto.request.CreateTransactionRequest;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.service.UserResourceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.transaction.validation.TransactionCategoryValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialAnalysisService {

    private static final String USER_REQUIRED_MESSAGE = "User is required";
    private static final String TOTAL_AMOUNT_INVALID_MESSAGE = "Total amount must be greater than zero";
    private static final String TOTAL_INSTALLMENTS_INVALID_MESSAGE = "Total installments must be at least 2";
    private static final String INSTALLMENT_ONLY_EXPENSE_MESSAGE =
            "Installment transactions are allowed only for expenses";

    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final UserResourceResolver userResourceResolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TransactionCategoryValidator transactionCategoryValidator;

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse getFinancialCommitment(Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        return buildCurrentAnalysis(user);
    }

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse getFinancialCommitment(User user) {
        if (user == null) {
            throw new IllegalArgumentException(USER_REQUIRED_MESSAGE);
        }

        return buildCurrentAnalysis(user);
    }

    @Transactional(readOnly = true)
    public InstallmentPurchaseCapacityResponse analyzeInstallmentPurchaseCapacity(
            User user,
            BigDecimal totalAmount,
            Integer totalInstallments
    ) {
        validateInstallmentPurchaseCapacityInput(user, totalAmount, totalInstallments);

        FinancialCommitmentResponse currentAnalysis = buildCurrentAnalysis(user);

        BigDecimal estimatedInstallmentAmount = totalAmount.divide(
                BigDecimal.valueOf(totalInstallments),
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal estimatedProjectedExpense = currentAnalysis.nextMonthProjectedExpense().add(estimatedInstallmentAmount);
        BigDecimal estimatedProjectedNet = currentAnalysis.nextMonthProjectedIncome().subtract(estimatedProjectedExpense);

        BigDecimal estimatedCommitmentPercentage = calculatePercentage(
                estimatedProjectedExpense,
                currentAnalysis.monthlyIncomeReference()
        );

        BigDecimal installmentIncomeRatio = calculatePercentage(
                estimatedInstallmentAmount,
                currentAnalysis.monthlyIncomeReference()
        );

        InstallmentPurchaseAnalysisResult analysisResult = resolveInstallmentPurchaseAnalysisResult(
                currentAnalysis,
                estimatedProjectedNet,
                estimatedCommitmentPercentage,
                installmentIncomeRatio
        );

        return new InstallmentPurchaseCapacityResponse(
                totalAmount,
                totalInstallments,
                estimatedInstallmentAmount,
                analysisResult.status(),
                analysisResult.observation()
        );
    }

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse previewTransactionAlert(
            CreateTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);

        validateAccountAndCategoryOwnership(
                request.accountId(),
                request.categoryId(),
                user.getId(),
                request.type()
        );

        FinancialCommitmentResponse currentAnalysis = buildCurrentAnalysis(user);

        BigDecimal totalFutureInstallments = currentAnalysis.totalFutureInstallments();
        BigDecimal nextMonthProjectedExpense = currentAnalysis.nextMonthProjectedExpense();
        BigDecimal monthlyBaseIncome = currentAnalysis.monthlyBaseIncome();
        BigDecimal monthlyIncomeReference = currentAnalysis.monthlyIncomeReference();
        BigDecimal projectedRecurringExpenseNextMonth = currentAnalysis.projectedRecurringExpenseNextMonth();
        BigDecimal projectedRecurringIncomeNextMonth = currentAnalysis.projectedRecurringIncomeNextMonth();
        Long activeInstallmentCount = currentAnalysis.activeInstallmentCount();

        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        if (request.type() == TransactionType.EXPENSE && YearMonth.from(request.date()).equals(nextMonth)) {
            nextMonthProjectedExpense = nextMonthProjectedExpense.add(request.amount());
        }

        if (request.type() == TransactionType.INCOME && YearMonth.from(request.date()).equals(nextMonth)) {
            projectedRecurringIncomeNextMonth = projectedRecurringIncomeNextMonth.add(request.amount());
        }

        return buildResponse(
                totalFutureInstallments,
                nextMonthProjectedExpense,
                monthlyBaseIncome,
                monthlyIncomeReference,
                projectedRecurringExpenseNextMonth,
                projectedRecurringIncomeNextMonth,
                activeInstallmentCount
        );
    }

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse previewInstallmentAlert(
            CreateInstallmentTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);

        validateInstallmentRequest(request);

        validateAccountAndCategoryOwnership(
                request.accountId(),
                request.categoryId(),
                user.getId(),
                request.type()
        );

        FinancialCommitmentResponse currentAnalysis = buildCurrentAnalysis(user);

        BigDecimal totalFutureInstallments = currentAnalysis.totalFutureInstallments();
        BigDecimal nextMonthProjectedExpense = currentAnalysis.nextMonthProjectedExpense();
        BigDecimal monthlyBaseIncome = currentAnalysis.monthlyBaseIncome();
        BigDecimal monthlyIncomeReference = currentAnalysis.monthlyIncomeReference();
        BigDecimal projectedRecurringExpenseNextMonth = currentAnalysis.projectedRecurringExpenseNextMonth();
        BigDecimal projectedRecurringIncomeNextMonth = currentAnalysis.projectedRecurringIncomeNextMonth();
        Long activeInstallmentCount = currentAnalysis.activeInstallmentCount();

        InstallmentProjection projection = calculateInstallmentProjection(
                request,
                totalFutureInstallments,
                nextMonthProjectedExpense
        );

        activeInstallmentCount = activeInstallmentCount + 1;

        return buildResponse(
                projection.totalFutureInstallments(),
                projection.nextMonthProjectedExpense(),
                monthlyBaseIncome,
                monthlyIncomeReference,
                projectedRecurringExpenseNextMonth,
                projectedRecurringIncomeNextMonth,
                activeInstallmentCount
        );
    }

    private void validateInstallmentPurchaseCapacityInput(
            User user,
            BigDecimal totalAmount,
            Integer totalInstallments
    ) {
        if (user == null) {
            throw new IllegalArgumentException(USER_REQUIRED_MESSAGE);
        }

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(TOTAL_AMOUNT_INVALID_MESSAGE);
        }

        if (totalInstallments == null || totalInstallments < 2) {
            throw new IllegalArgumentException(TOTAL_INSTALLMENTS_INVALID_MESSAGE);
        }
    }

    private void validateInstallmentRequest(CreateInstallmentTransactionRequest request) {
        if (request.type() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException(INSTALLMENT_ONLY_EXPENSE_MESSAGE);
        }

        if (request.totalInstallments() == null || request.totalInstallments() < 2) {
            throw new IllegalArgumentException(TOTAL_INSTALLMENTS_INVALID_MESSAGE);
        }
    }

    private void validateAccountAndCategoryOwnership(
            Long accountId,
            Long categoryId,
            Long userId,
            TransactionType transactionType
    ) {
        userResourceResolver.resolveAccount(accountId, userId);

        Category category = userResourceResolver.resolveCategory(categoryId, userId);

        transactionCategoryValidator.validate(category, transactionType);
    }

    private BigDecimal calculatePercentage(BigDecimal value, BigDecimal reference) {
        if (reference.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return value
                .multiply(BigDecimal.valueOf(100))
                .divide(reference, 2, RoundingMode.HALF_UP);
    }

    private InstallmentPurchaseAnalysisResult resolveInstallmentPurchaseAnalysisResult(
            FinancialCommitmentResponse currentAnalysis,
            BigDecimal estimatedProjectedNet,
            BigDecimal estimatedCommitmentPercentage,
            BigDecimal installmentIncomeRatio
    ) {
        if (isDesfavoravelScenario(currentAnalysis, estimatedProjectedNet, estimatedCommitmentPercentage)) {
            String observation = currentAnalysis.monthlyIncomeReference().compareTo(BigDecimal.ZERO) <= 0
                    ? "Sem renda de referência configurada, a análise fica conservadora e desfavorável."
                    : "A nova parcela tende a pressionar demais seu orçamento no cenário atual.";

            return new InstallmentPurchaseAnalysisResult("DESFAVORAVEL", observation);
        }

        if (isAlertaScenario(currentAnalysis, estimatedCommitmentPercentage, installmentIncomeRatio)) {
            return new InstallmentPurchaseAnalysisResult(
                    "ALERTA",
                    "A compra pode caber, mas aumenta o comprometimento do orçamento desde a parcela atual."
            );
        }

        return new InstallmentPurchaseAnalysisResult(
                "VIAVEL",
                "A parcela estimada ainda parece compatível com seu cenário financeiro atual."
        );
    }

    private boolean isDesfavoravelScenario(
            FinancialCommitmentResponse currentAnalysis,
            BigDecimal estimatedProjectedNet,
            BigDecimal estimatedCommitmentPercentage
    ) {
        return "HIGH".equalsIgnoreCase(currentAnalysis.riskLevel())
                || currentAnalysis.monthlyIncomeReference().compareTo(BigDecimal.ZERO) <= 0
                || estimatedProjectedNet.compareTo(BigDecimal.ZERO) < 0
                || estimatedCommitmentPercentage.compareTo(BigDecimal.valueOf(80)) >= 0;
    }

    private boolean isAlertaScenario(
            FinancialCommitmentResponse currentAnalysis,
            BigDecimal estimatedCommitmentPercentage,
            BigDecimal installmentIncomeRatio
    ) {
        return "MEDIUM".equalsIgnoreCase(currentAnalysis.riskLevel())
                || estimatedCommitmentPercentage.compareTo(BigDecimal.valueOf(60)) >= 0
                || installmentIncomeRatio.compareTo(BigDecimal.valueOf(20)) >= 0;
    }

    private InstallmentProjection calculateInstallmentProjection(
            CreateInstallmentTransactionRequest request,
            BigDecimal totalFutureInstallments,
            BigDecimal nextMonthProjectedExpense
    ) {
        int totalInstallments = request.totalInstallments();
        BigDecimal totalAmount = request.totalAmount();

        BigDecimal installmentAmount = totalAmount.divide(
                BigDecimal.valueOf(totalInstallments),
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal accumulated = BigDecimal.ZERO;
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= totalInstallments; i++) {
            BigDecimal currentAmount = calculateCurrentInstallmentAmount(
                    i,
                    totalInstallments,
                    installmentAmount,
                    totalAmount,
                    accumulated
            );

            if (i < totalInstallments) {
                accumulated = accumulated.add(currentAmount);
            }

            LocalDate installmentDate = request.firstInstallmentDate().plusMonths((long) i - 1);

            if (installmentDate.isAfter(today)) {
                totalFutureInstallments = totalFutureInstallments.add(currentAmount);
            }

            if (YearMonth.from(installmentDate).equals(nextMonth)) {
                nextMonthProjectedExpense = nextMonthProjectedExpense.add(currentAmount);
            }
        }

        return new InstallmentProjection(totalFutureInstallments, nextMonthProjectedExpense);
    }

    private BigDecimal calculateCurrentInstallmentAmount(
            int currentInstallment,
            int totalInstallments,
            BigDecimal installmentAmount,
            BigDecimal totalAmount,
            BigDecimal accumulated
    ) {
        if (currentInstallment < totalInstallments) {
            return installmentAmount;
        }

        return totalAmount.subtract(accumulated);
    }

    private FinancialCommitmentResponse buildCurrentAnalysis(User user) {
        LocalDate today = LocalDate.now();

        BigDecimal totalFutureInstallments = transactionRepository.sumFutureInstallmentsByUser(user.getId(), today);

        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        LocalDate nextMonthStart = nextMonth.atDay(1);
        LocalDate nextMonthEnd = nextMonth.atEndOfMonth();

        BigDecimal nextMonthProjectedExpense =
                transactionRepository.sumProjectedExpensesBetweenDatesByUser(
                        user.getId(),
                        nextMonthStart,
                        nextMonthEnd
                );

        BigDecimal monthlyBaseIncome = user.getMonthlyBaseIncome();
        BigDecimal monthlyIncomeReference = resolveMonthlyIncomeReference(user);

        Long activeInstallmentCount =
                transactionRepository.countDistinctActiveInstallmentGroupsByUser(user.getId(), today);

        RecurringProjection recurringProjection = calculateRecurringProjectionForNextMonth(
                user,
                nextMonthStart,
                nextMonthEnd
        );

        nextMonthProjectedExpense = nextMonthProjectedExpense.add(recurringProjection.recurringExpense());

        return buildResponse(
                totalFutureInstallments,
                nextMonthProjectedExpense,
                monthlyBaseIncome,
                monthlyIncomeReference,
                recurringProjection.recurringExpense(),
                recurringProjection.recurringIncome(),
                activeInstallmentCount
        );
    }

    private BigDecimal resolveMonthlyIncomeReference(User user) {
        if (user.getMonthlyBaseIncome() != null && user.getMonthlyBaseIncome().compareTo(BigDecimal.ZERO) > 0) {
            return user.getMonthlyBaseIncome();
        }

        YearMonth currentMonth = YearMonth.now();
        LocalDate currentMonthStart = currentMonth.atDay(1);
        LocalDate currentMonthEnd = currentMonth.atEndOfMonth();

        return transactionRepository.sumIncomeBetweenDatesByUser(
                user.getId(),
                currentMonthStart,
                currentMonthEnd
        );
    }

    private RecurringProjection calculateRecurringProjectionForNextMonth(
            User user,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<RecurringTransaction> recurringTransactions =
                recurringTransactionRepository.findAllByUserIdAndActiveTrue(user.getId());

        BigDecimal recurringExpense = BigDecimal.ZERO;
        BigDecimal recurringIncome = BigDecimal.ZERO;

        for (RecurringTransaction recurringTransaction : recurringTransactions) {
            if (hasOccurrenceInPeriod(recurringTransaction, startDate, endDate)) {
                if (recurringTransaction.getType() == TransactionType.EXPENSE) {
                    recurringExpense = recurringExpense.add(recurringTransaction.getAmount());
                } else if (recurringTransaction.getType() == TransactionType.INCOME) {
                    recurringIncome = recurringIncome.add(recurringTransaction.getAmount());
                }
            }
        }

        return new RecurringProjection(recurringExpense, recurringIncome);
    }

    private boolean hasOccurrenceInPeriod(
            RecurringTransaction recurringTransaction,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        if (!recurringTransaction.isActive()) {
            return false;
        }

        LocalDate occurrenceDate = recurringTransaction.getNextExecutionDate();

        if (occurrenceDate == null) {
            occurrenceDate = recurringTransaction.getStartDate();
        }

        if (occurrenceDate == null) {
            return false;
        }

        while (!occurrenceDate.isAfter(periodEnd)) {
            if (!occurrenceDate.isBefore(periodStart) && isWithinRecurrenceRange(recurringTransaction, occurrenceDate)) {
                return true;
            }

            occurrenceDate = getNextOccurrenceDate(occurrenceDate, recurringTransaction);
        }

        return false;
    }

    private boolean isWithinRecurrenceRange(RecurringTransaction recurringTransaction, LocalDate date) {
        if (date.isBefore(recurringTransaction.getStartDate())) {
            return false;
        }

        return recurringTransaction.getEndDate() == null || !date.isAfter(recurringTransaction.getEndDate());
    }

    private LocalDate getNextOccurrenceDate(LocalDate currentDate, RecurringTransaction recurringTransaction) {
        return switch (recurringTransaction.getFrequency()) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            case YEARLY -> currentDate.plusYears(1);
        };
    }

    private FinancialCommitmentResponse buildResponse(
            BigDecimal totalFutureInstallments,
            BigDecimal nextMonthProjectedExpense,
            BigDecimal monthlyBaseIncome,
            BigDecimal monthlyIncomeReference,
            BigDecimal projectedRecurringExpenseNextMonth,
            BigDecimal projectedRecurringIncomeNextMonth,
            Long activeInstallmentCount
    ) {
        if (totalFutureInstallments == null) {
            totalFutureInstallments = BigDecimal.ZERO;
        }

        if (nextMonthProjectedExpense == null) {
            nextMonthProjectedExpense = BigDecimal.ZERO;
        }

        if (monthlyBaseIncome == null) {
            monthlyBaseIncome = BigDecimal.ZERO;
        }

        if (monthlyIncomeReference == null) {
            monthlyIncomeReference = BigDecimal.ZERO;
        }

        if (projectedRecurringExpenseNextMonth == null) {
            projectedRecurringExpenseNextMonth = BigDecimal.ZERO;
        }

        if (projectedRecurringIncomeNextMonth == null) {
            projectedRecurringIncomeNextMonth = BigDecimal.ZERO;
        }

        if (activeInstallmentCount == null) {
            activeInstallmentCount = 0L;
        }

        BigDecimal nextMonthProjectedIncome = monthlyIncomeReference.add(projectedRecurringIncomeNextMonth);
        BigDecimal projectedNetNextMonth = nextMonthProjectedIncome.subtract(nextMonthProjectedExpense);

        BigDecimal commitmentPercentage = BigDecimal.ZERO;

        if (monthlyIncomeReference.compareTo(BigDecimal.ZERO) > 0) {
            commitmentPercentage = nextMonthProjectedExpense
                    .multiply(BigDecimal.valueOf(100))
                    .divide(monthlyIncomeReference, 2, RoundingMode.HALF_UP);
        }

        boolean excessiveInstallments = activeInstallmentCount >= 5;
        boolean tightBudgetRisk = commitmentPercentage.compareTo(BigDecimal.valueOf(60)) >= 0;
        boolean highRisk = commitmentPercentage.compareTo(BigDecimal.valueOf(80)) >= 0
                || activeInstallmentCount >= 8
                || projectedNetNextMonth.compareTo(BigDecimal.ZERO) < 0;

        boolean riskDetected = excessiveInstallments || tightBudgetRisk || highRisk;

        String riskLevel;
        String message;

        if (highRisk) {
            riskLevel = "HIGH";
            message = "Alto risco financeiro detectado. Suas despesas projetadas podem comprometer significativamente seu orçamento.";
        } else if (riskDetected) {
            riskLevel = "MEDIUM";
            message = "Atenção: seu comprometimento financeiro está aumentando. Revise despesas futuras e parcelamentos.";
        } else {
            riskLevel = "LOW";
            message = "Seu comprometimento financeiro está sob controle.";
        }

        if (monthlyIncomeReference.compareTo(BigDecimal.ZERO) == 0) {
            message = "Renda de referência não configurada. Defina sua renda mensal base para uma análise mais precisa.";
        }

        return new FinancialCommitmentResponse(
                totalFutureInstallments,
                nextMonthProjectedExpense,
                monthlyBaseIncome,
                monthlyIncomeReference,
                projectedRecurringExpenseNextMonth,
                projectedRecurringIncomeNextMonth,
                nextMonthProjectedIncome,
                projectedNetNextMonth,
                commitmentPercentage,
                activeInstallmentCount,
                excessiveInstallments,
                tightBudgetRisk,
                riskDetected,
                riskLevel,
                message
        );
    }

    private record InstallmentPurchaseAnalysisResult(
            String status,
            String observation
    ) {
    }

    private record InstallmentProjection(
            BigDecimal totalFutureInstallments,
            BigDecimal nextMonthProjectedExpense
    ) {
    }

    private record RecurringProjection(
            BigDecimal recurringExpense,
            BigDecimal recurringIncome
    ) {
    }
}