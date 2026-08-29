package com.financebot.telegram.application.usecase;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.telegram.dto.response.MonthlyAmountSummaryResponse;
import com.financebot.telegram.dto.response.TelegramActiveInstallmentSummaryResponse;
import com.financebot.telegram.dto.response.TelegramActiveInstallmentsResponse;
import com.financebot.telegram.dto.response.TelegramInstallmentCountResponse;
import com.financebot.telegram.dto.response.TelegramTransactionSummaryResponse;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
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
public class TelegramFinancialQueryApplicationUseCase {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialAnalysisService financialAnalysisService;

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse financialAnalysis(Long telegramId) {
        return financialAnalysisService.getFinancialCommitment(findUser(telegramId));
    }

    @Transactional(readOnly = true)
    public MonthlyAmountSummaryResponse currentMonthSummary(Long telegramId, TransactionType type) {
        User user = findUser(telegramId);
        YearMonth month = YearMonth.now();
        BigDecimal total = transactionRepository.sumAmountByUserAndTypeBetweenDates(
                user.getId(), type, month.atDay(1), month.atEndOfMonth()
        );
        return new MonthlyAmountSummaryResponse(type.name(), month.toString(), total);
    }

    @Transactional(readOnly = true)
    public TelegramTransactionSummaryResponse transactionSummary(
            Long telegramId, TransactionType type, String requestedType, String categoryName,
            String accountName, LocalDate startDate, LocalDate endDate
    ) {
        User user = findUser(telegramId);
        String category = normalizeBlankToNull(categoryName);
        String account = normalizeBlankToNull(accountName);
        BigDecimal total;

        if (category != null && account != null) {
            total = transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndCategoryAndAccount(
                    user.getId(), type, startDate, endDate, category, account);
        } else if (category != null) {
            total = transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndCategory(
                    user.getId(), type, startDate, endDate, category);
        } else if (account != null) {
            total = transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndAccount(
                    user.getId(), type, startDate, endDate, account);
        } else {
            total = transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                    user.getId(), type, startDate, endDate);
        }

        return new TelegramTransactionSummaryResponse(
                requestedType, category, account, startDate, endDate, total
        );
    }

    @Transactional(readOnly = true)
    public TelegramInstallmentCountResponse installmentCount(Long telegramId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new EntityNotFoundException("User not found for telegramId"));
        Long count = transactionRepository.countInstallmentsByUserBetweenDates(user.getId(), startDate, endDate);
        return new TelegramInstallmentCountResponse(count != null ? count : 0L, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public InstallmentPurchaseCapacityResponse installmentCapacity(Long telegramId, BigDecimal totalAmount, Integer totalInstallments) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total amount must be greater than zero");
        }
        if (totalInstallments == null || totalInstallments < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total installments must be at least 2");
        }
        return financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                findUser(telegramId), totalAmount, totalInstallments
        );
    }

    @Transactional(readOnly = true)
    public TelegramActiveInstallmentsResponse activeInstallments(Long telegramId) {
        User user = findUser(telegramId);
        Long count = transactionRepository.countDistinctActiveInstallmentGroupsByUser(user.getId(), LocalDate.now());
        return new TelegramActiveInstallmentsResponse(count != null ? count : 0L);
    }

    @Transactional(readOnly = true)
    public TelegramActiveInstallmentSummaryResponse activeInstallmentSummary(Long telegramId, String query) {
        User user = findUser(telegramId);
        List<Transaction> active = transactionRepository.findActiveInstallmentTransactionsByUser(user.getId(), LocalDate.now());
        if (active.isEmpty()) return emptySummary();

        Map<String, List<Transaction>> grouped = active.stream()
                .collect(Collectors.groupingBy(Transaction::getInstallmentGroupId));
        if (query != null && !query.isBlank()) {
            String normalizedQuery = normalizeSearch(query);
            grouped = grouped.entrySet().stream().filter(entry -> {
                Transaction first = entry.getValue().get(0);
                return normalizeSearch(stripSuffix(first.getDescription())).contains(normalizedQuery);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        if (grouped.isEmpty()) return emptySummary();
        if (grouped.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple active installments found");

        String groupId = grouped.keySet().iterator().next();
        List<Transaction> transactions = transactionRepository.findInstallmentTransactionsByGroupIdAndUser(user.getId(), groupId);
        Transaction reference = transactions.get(0);
        Transaction current = transactions.stream()
                .filter(t -> t.getDate() != null && !t.getDate().isAfter(LocalDate.now()))
                .max((a, b) -> a.getDate().compareTo(b.getDate())).orElse(null);
        List<Transaction> future = transactions.stream()
                .filter(t -> t.getDate() != null && t.getDate().isAfter(LocalDate.now())).toList();
        Transaction firstUpcoming = future.isEmpty() ? null : future.get(0);
        int total = reference.getTotalInstallments() != null ? reference.getTotalInstallments() : transactions.size();
        LocalDate endDate = transactions.stream().map(Transaction::getDate).max(LocalDate::compareTo).orElse(reference.getDate());

        return new TelegramActiveInstallmentSummaryResponse(
                true, reference.getInstallmentGroupId(), stripSuffix(reference.getDescription()),
                current != null ? current.getDate() : null, current != null ? current.getInstallmentNumber() : null,
                firstUpcoming != null ? firstUpcoming.getDate() : null,
                firstUpcoming != null ? firstUpcoming.getInstallmentNumber() : null,
                total, future.size(), endDate
        );
    }

    private User findUser(Long telegramId) {
        return userRepository.findByTelegramId(telegramId).orElseThrow(TelegramUserNotFoundException::new);
    }

    private TelegramActiveInstallmentSummaryResponse emptySummary() {
        return new TelegramActiveInstallmentSummaryResponse(false, null, null, null, null, null, null, null, 0, null);
    }

    private String normalizeBlankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String stripSuffix(String description) {
        if (description == null || description.isBlank()) return description;
        String trimmed = description.trim();
        int dash = trimmed.lastIndexOf('-');
        if (dash < 0) return trimmed;
        String suffix = trimmed.substring(dash + 1).trim();
        String[] parts = suffix.split("/");
        if (parts.length != 2 || !positiveInteger(parts[0]) || !positiveInteger(parts[1])) return trimmed;
        return trimmed.substring(0, dash).trim();
    }

    private boolean positiveInteger(String value) {
        return value != null && !value.isBlank() && value.chars().allMatch(Character::isDigit);
    }

    private String normalizeSearch(String value) {
        if (value == null) return "";
        return value.toLowerCase().replace("á", "a").replace("à", "a").replace("ã", "a")
                .replace("â", "a").replace("é", "e").replace("ê", "e").replace("í", "i")
                .replace("ó", "o").replace("ô", "o").replace("õ", "o").replace("ú", "u")
                .replace("ç", "c").replaceAll("\\s+", " ").trim();
    }
}
