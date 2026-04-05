package com.financebot.recurring.domain;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_transactions")
@Getter
@Setter
@NoArgsConstructor
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_execution_date", nullable = false)
    private LocalDate nextExecutionDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (nextExecutionDate == null) {
            nextExecutionDate = startDate;
        }
    }

    public void validateDates() {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    public boolean isActiveOn(LocalDate date) {
        if (!active) {
            return false;
        }

        if (date.isBefore(startDate)) {
            return false;
        }

        return endDate == null || !date.isAfter(endDate);
    }

    public boolean isDueOnOrBefore(LocalDate date) {
        return active
                && nextExecutionDate != null
                && !nextExecutionDate.isAfter(date)
                && (endDate == null || !nextExecutionDate.isAfter(endDate));
    }

    public void advanceNextExecutionDate() {
        if (nextExecutionDate == null) {
            nextExecutionDate = startDate;
            return;
        }

        nextExecutionDate = switch (frequency) {
            case DAILY -> nextExecutionDate.plusDays(1);
            case WEEKLY -> nextExecutionDate.plusWeeks(1);
            case MONTHLY -> nextExecutionDate.plusMonths(1);
            case YEARLY -> nextExecutionDate.plusYears(1);
        };
    }
}