package com.financebot.reminder.mapper;

import com.financebot.reminder.domain.Reminder;
import com.financebot.reminder.dto.response.ReminderResponse;
import org.springframework.stereotype.Component;

@Component
public class ReminderMapper {

    public ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getDescription(),
                reminder.getReminderDate(),
                reminder.getDaysBefore(),
                reminder.isActive(),
                reminder.getSentAt(),
                reminder.getRecurringTransactionId()
        );
    }
}
