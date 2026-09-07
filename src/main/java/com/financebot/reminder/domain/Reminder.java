package com.financebot.reminder.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Reminder {

    private Long id;
    private String description;
    private LocalDate reminderDate;
    private int daysBefore;
    private boolean active = true;
    private LocalDateTime sentAt;
    private LocalDateTime claimedAt;
    private LocalDateTime createdAt;
    private Long userId;
    private Long telegramId;
    private Long recurringTransactionId;

    public void markSent() { sentAt = LocalDateTime.now(); claimedAt = null; active = false; }
    public void reschedule(LocalDate date) { reminderDate = date; claimedAt = null; sentAt = null; active = true; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getReminderDate() { return reminderDate; }
    public void setReminderDate(LocalDate reminderDate) { this.reminderDate = reminderDate; }
    public int getDaysBefore() { return daysBefore; }
    public void setDaysBefore(int daysBefore) { this.daysBefore = daysBefore; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }
    public Long getRecurringTransactionId() { return recurringTransactionId; }
    public void setRecurringTransactionId(Long recurringTransactionId) { this.recurringTransactionId = recurringTransactionId; }
}
