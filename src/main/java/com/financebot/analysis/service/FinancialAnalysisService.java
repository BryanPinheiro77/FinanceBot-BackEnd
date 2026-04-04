package com.financebot.analysis.service;

import com.financebot.account.domain.Account;
import com.financebot.account.repository.AccountRepository;
import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.dto.request.CreateTransactionRequest;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class FinancialAnalysisService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse getFinancialCommitment(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return buildCurrentAnalysis(user);
    }

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse previewTransactionAlert(
            CreateTransactionRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        Account account = getUserAccount(request.accountId(), user.getId());
        Category category = getUserCategory(request.categoryId(), user.getId());

        validateCategoryMatchesTransactionType(category, request.type());

        FinancialCommitmentResponse currentAnalysis = buildCurrentAnalysis(user);

        BigDecimal totalFutureInstallments = currentAnalysis.totalFutureInstallments();
        BigDecimal nextMonthProjectedExpense = currentAnalysis.nextMonthProjectedExpense();
        BigDecimal monthlyIncomeReference = currentAnalysis.monthlyIncomeReference();
        Long activeInstallmentCount = currentAnalysis.activeInstallmentCount();

        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        if (request.type() == TransactionType.EXPENSE && YearMonth.from(request.date()).equals(nextMonth)) {
            nextMonthProjectedExpense = nextMonthProjectedExpense.add(request.amount());
        }

        return buildResponse(
                totalFutureInstallments,
                nextMonthProjectedExpense,
                monthlyIncomeReference,
                activeInstallmentCount
        );
    }

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse previewInstallmentAlert(
            CreateInstallmentTransactionRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        if (request.type() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Installment transactions are allowed only for expenses");
        }

        if (request.totalInstallments() == null || request.totalInstallments() < 2) {
            throw new IllegalArgumentException("Total installments must be at least 2");
        }

        Account account = getUserAccount(request.accountId(), user.getId());
        Category category = getUserCategory(request.categoryId(), user.getId());

        validateCategoryMatchesTransactionType(category, request.type());

        FinancialCommitmentResponse currentAnalysis = buildCurrentAnalysis(user);

        BigDecimal totalFutureInstallments = currentAnalysis.totalFutureInstallments();
        BigDecimal nextMonthProjectedExpense = currentAnalysis.nextMonthProjectedExpense();
        BigDecimal monthlyIncomeReference = currentAnalysis.monthlyIncomeReference();
        Long activeInstallmentCount = currentAnalysis.activeInstallmentCount();

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
            BigDecimal currentAmount = installmentAmount;

            if (i < totalInstallments) {
                accumulated = accumulated.add(currentAmount);
            } else {
                currentAmount = totalAmount.subtract(accumulated);
            }

            LocalDate installmentDate = request.firstInstallmentDate().plusMonths(i - 1);

            if (installmentDate.isAfter(today)) {
                totalFutureInstallments = totalFutureInstallments.add(currentAmount);
            }

            if (YearMonth.from(installmentDate).equals(nextMonth)) {
                nextMonthProjectedExpense = nextMonthProjectedExpense.add(currentAmount);
            }
        }

        activeInstallmentCount = activeInstallmentCount + 1;

        return buildResponse(
                totalFutureInstallments,
                nextMonthProjectedExpense,
                monthlyIncomeReference,
                activeInstallmentCount
        );
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

        BigDecimal monthlyIncomeReference = resolveMonthlyIncomeReference(user);

        Long activeInstallmentCount =
                transactionRepository.countDistinctActiveInstallmentGroupsByUser(user.getId(), today);

        return buildResponse(
                totalFutureInstallments,
                nextMonthProjectedExpense,
                monthlyIncomeReference,
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

    private FinancialCommitmentResponse buildResponse(
            BigDecimal totalFutureInstallments,
            BigDecimal nextMonthProjectedExpense,
            BigDecimal monthlyIncomeReference,
            Long activeInstallmentCount
    ) {
        if (totalFutureInstallments == null) {
            totalFutureInstallments = BigDecimal.ZERO;
        }

        if (nextMonthProjectedExpense == null) {
            nextMonthProjectedExpense = BigDecimal.ZERO;
        }

        if (monthlyIncomeReference == null) {
            monthlyIncomeReference = BigDecimal.ZERO;
        }

        if (activeInstallmentCount == null) {
            activeInstallmentCount = 0L;
        }

        BigDecimal commitmentPercentage = BigDecimal.ZERO;

        if (monthlyIncomeReference.compareTo(BigDecimal.ZERO) > 0) {
            commitmentPercentage = nextMonthProjectedExpense
                    .multiply(BigDecimal.valueOf(100))
                    .divide(monthlyIncomeReference, 2, RoundingMode.HALF_UP);
        }

        boolean excessiveInstallments = activeInstallmentCount >= 5;
        boolean tightBudgetRisk = commitmentPercentage.compareTo(BigDecimal.valueOf(60)) >= 0;
        boolean highRisk = commitmentPercentage.compareTo(BigDecimal.valueOf(80)) >= 0
                || activeInstallmentCount >= 8;
        boolean riskDetected = excessiveInstallments || tightBudgetRisk || highRisk;

        String riskLevel;
        String message;

        if (highRisk) {
            riskLevel = "HIGH";
            message = "High financial risk detected. Your projected expenses may significantly impact your budget.";
        } else if (riskDetected) {
            riskLevel = "MEDIUM";
            message = "Attention: your financial commitment is rising. Review future expenses and installments.";
        } else {
            riskLevel = "LOW";
            message = "Your financial commitment is under control.";
        }

        if (monthlyIncomeReference.compareTo(BigDecimal.ZERO) == 0) {
            message = "Income reference not configured. Set your monthly base income for a more accurate analysis.";
        }

        return new FinancialCommitmentResponse(
                totalFutureInstallments,
                nextMonthProjectedExpense,
                monthlyIncomeReference,
                commitmentPercentage,
                activeInstallmentCount,
                excessiveInstallments,
                tightBudgetRisk,
                riskDetected,
                riskLevel,
                message
        );
    }

    private void validateCategoryMatchesTransactionType(Category category, TransactionType transactionType) {
        boolean isIncomeMatch =
                category.getType() == CategoryType.INCOME && transactionType == TransactionType.INCOME;

        boolean isExpenseMatch =
                category.getType() == CategoryType.EXPENSE && transactionType == TransactionType.EXPENSE;

        if (!isIncomeMatch && !isExpenseMatch) {
            throw new IllegalArgumentException("Category type does not match transaction type");
        }
    }

    private Account getUserAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    private Category getUserCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is invalid");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }
}