package com.financebot.telegram.service;

import com.financebot.account.domain.Account;
import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.category.domain.Category;
import com.financebot.telegram.dto.*;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.service.TransactionService;
import com.financebot.user.domain.User;
import com.financebot.user.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.user.dto.response.TelegramUserProfileResponse;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelegramIntegrationService {

    private final UserRepository userRepository;
    private final FinancialAnalysisService financialAnalysisService;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final TelegramAccountResolverService telegramAccountResolverService;
    private final TelegramCategoryResolverService telegramCategoryResolverService;

    @Transactional(readOnly = true)
    public TelegramUserProfileResponse getMe(Long telegramId) {
        User user = findUserByTelegramId(telegramId);

        return new TelegramUserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMonthlyBaseIncome(),
                user.getTelegramId()
        );
    }

    @Transactional
    public TelegramUserProfileResponse updateMonthlyBaseIncome(
            Long telegramId,
            UpdateMonthlyBaseIncomeRequest request
    ) {
        User user = findUserByTelegramId(telegramId);
        user.setMonthlyBaseIncome(request.monthlyBaseIncome());

        User savedUser = userRepository.save(user);

        return new TelegramUserProfileResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getMonthlyBaseIncome(),
                savedUser.getTelegramId()
        );
    }

    @Transactional
    public void disconnectTelegram(Long telegramId) {
        User user = findUserByTelegramId(telegramId);
        user.setTelegramId(null);
        user.setTelegramLinkCode(null);
        user.setTelegramLinkCodeExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse getFinancialAnalysis(Long telegramId) {
        User user = findUserByTelegramId(telegramId);
        return financialAnalysisService.getFinancialCommitment(user);
    }

    private User findUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(TelegramUserNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public MonthlyAmountSummaryResponse getCurrentMonthExpenseSummary(Long telegramId) {
        User user = findUserByTelegramId(telegramId);

        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        BigDecimal totalAmount = transactionRepository.sumAmountByUserAndTypeBetweenDates(
                user.getId(),
                TransactionType.EXPENSE,
                startDate,
                endDate
        );

        return new MonthlyAmountSummaryResponse(
                "EXPENSE",
                currentMonth.toString(),
                totalAmount
        );
    }

    @Transactional(readOnly = true)
    public MonthlyAmountSummaryResponse getCurrentMonthIncomeSummary(Long telegramId) {
        User user = findUserByTelegramId(telegramId);

        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        BigDecimal totalAmount = transactionRepository.sumAmountByUserAndTypeBetweenDates(
                user.getId(),
                TransactionType.INCOME,
                startDate,
                endDate
        );

        return new MonthlyAmountSummaryResponse(
                "INCOME",
                currentMonth.toString(),
                totalAmount
        );
    }

    @Transactional
    public void createTransactionFromTelegram(CreateTransactionFromTelegramRequest request) {
        User user = findUserByTelegramId(request.telegramId());
        TransactionType transactionType = TransactionType.valueOf(request.type());

        Account account = telegramAccountResolverService.resolve(user, request.accountName());

        Category category = resolveCategoryFromRequest(
                user,
                transactionType,
                request.categoryName(),
                request.description()
        );

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setDate(request.date());
        transaction.setType(transactionType);
        transaction.setSourceType(SourceType.BOT_TEXT);

        transactionRepository.save(transaction);
    }

    @Transactional
    public void createInstallmentTransactionFromTelegram(CreateInstallmentTransactionFromTelegramRequest request) {
        User user = findUserByTelegramId(request.telegramId());

        Account account = telegramAccountResolverService.resolve(user, request.accountName());
        Category category = resolveCategoryFromRequest(
                user,
                TransactionType.EXPENSE,
                request.categoryName(),
                request.description()
        );

        CreateInstallmentTransactionRequest installmentRequest = new CreateInstallmentTransactionRequest(
                request.totalAmount(),
                request.description(),
                request.firstInstallmentDate(),
                TransactionType.EXPENSE,
                SourceType.BOT_TEXT,
                account.getId(),
                category.getId(),
                request.totalInstallments()
        );

        transactionService.createInstallmentForUser(installmentRequest, user);
    }

    @Transactional(readOnly = true)
    public TelegramTransactionSummaryResponse getTransactionSummary(TelegramTransactionSummaryRequest request) {
        User user = findUserByTelegramId(request.telegramId());
        TransactionType type = TransactionType.valueOf(request.type());

        String categoryName = normalizeBlankToNull(request.categoryName());
        String accountName = normalizeBlankToNull(request.accountName());

        BigDecimal totalAmount;

        if (categoryName != null && accountName != null) {
            totalAmount = transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndCategoryAndAccount(
                    user.getId(),
                    type,
                    request.startDate(),
                    request.endDate(),
                    categoryName,
                    accountName
            );
        } else if (categoryName != null) {
            totalAmount = transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndCategory(
                    user.getId(),
                    type,
                    request.startDate(),
                    request.endDate(),
                    categoryName
            );
        } else if (accountName != null) {
            totalAmount = transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndAccount(
                    user.getId(),
                    type,
                    request.startDate(),
                    request.endDate(),
                    accountName
            );
        } else {
            totalAmount = transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                    user.getId(),
                    type,
                    request.startDate(),
                    request.endDate()
            );
        }

        return new TelegramTransactionSummaryResponse(
                request.type(),
                categoryName,
                accountName,
                request.startDate(),
                request.endDate(),
                totalAmount
        );
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Category resolveCategoryFromRequest(
            User user,
            TransactionType transactionType,
            String categoryName,
            String description
    ) {
        if (categoryName != null && !categoryName.isBlank()) {
            return telegramCategoryResolverService.resolveExplicitCategory(
                    user,
                    transactionType,
                    categoryName
            );
        }

        return telegramCategoryResolverService.resolveCategory(
                user,
                transactionType,
                description
        );
    }

    public TelegramDefaultAccountResponse getDefaultAccount(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para este Telegram."));

        Account account = telegramAccountResolverService.resolveDefaultAccount(user);

        return new TelegramDefaultAccountResponse(
                account.getId(),
                account.getName()
        );
    }

    @Transactional(readOnly = true)
    public TelegramInstallmentCountResponse getInstallmentCount(TelegramInstallmentCountRequest request) {
        User user = userRepository.findByTelegramId(request.telegramId())
                .orElseThrow(() -> new EntityNotFoundException("User not found for telegramId"));

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        Long count = transactionRepository.countInstallmentsByUserBetweenDates(
                user.getId(),
                startDate,
                endDate
        );

        return new TelegramInstallmentCountResponse(
                count != null ? count : 0L,
                startDate,
                endDate
        );
    }

    @Transactional(readOnly = true)
    public TelegramActiveInstallmentsResponse getActiveInstallments(Long telegramId) {
        User user = findUserByTelegramId(telegramId);

        Long count = transactionRepository.countDistinctActiveInstallmentGroupsByUser(
                user.getId(),
                LocalDate.now()
        );

        return new TelegramActiveInstallmentsResponse(count != null ? count : 0L);
    }

    @Transactional(readOnly = true)
    public TelegramActiveInstallmentSummaryResponse getActiveInstallmentSummary(Long telegramId) {
        User user = findUserByTelegramId(telegramId);

        List<Transaction> activeInstallments = transactionRepository.findActiveInstallmentTransactionsByUser(
                user.getId(),
                LocalDate.now()
        );

        if (activeInstallments.isEmpty()) {
            return new TelegramActiveInstallmentSummaryResponse(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    null
            );
        }

        Map<String, List<Transaction>> grouped = activeInstallments.stream()
                .collect(Collectors.groupingBy(Transaction::getInstallmentGroupId));

        if (grouped.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple active installments found");
        }

        List<Transaction> transactions = grouped.values().iterator().next();
        Transaction firstUpcoming = transactions.get(0);

        int remainingInstallments = transactions.size();
        int nextInstallmentNumber = firstUpcoming.getInstallmentNumber() != null
                ? firstUpcoming.getInstallmentNumber()
                : 1;
        int totalInstallments = firstUpcoming.getTotalInstallments() != null
                ? firstUpcoming.getTotalInstallments()
                : remainingInstallments;
        LocalDate endDate = transactions.stream()
                .map(Transaction::getDate)
                .max(LocalDate::compareTo)
                .orElse(firstUpcoming.getDate());

        return new TelegramActiveInstallmentSummaryResponse(
                true,
                firstUpcoming.getInstallmentGroupId(),
                stripInstallmentSuffix(firstUpcoming.getDescription()),
                firstUpcoming.getDate(),
                nextInstallmentNumber,
                totalInstallments,
                remainingInstallments,
                endDate
        );
    }

    private String stripInstallmentSuffix(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }

        return description.replaceFirst("\\s*-\\s*\\d+/\\d+\\s*$", "").trim();
    }
}
