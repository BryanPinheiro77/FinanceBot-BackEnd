package com.financebot.reminder.controller;

import com.financebot.reminder.dto.response.PendingReminderResponse;
import com.financebot.reminder.dto.request.CreateTelegramReminderRequest;
import com.financebot.reminder.dto.response.ReminderResponse;
import com.financebot.reminder.application.command.CreateTelegramReminderCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.financebot.reminder.application.usecase.ReminderUseCase;
import com.financebot.reminder.mapper.ReminderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/telegram/reminders")
@RequiredArgsConstructor
public class TelegramReminderController {

    private final ReminderUseCase reminderUseCase;
    private final ReminderMapper reminderMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(@RequestBody @Valid CreateTelegramReminderRequest request) {
        return reminderMapper.toResponse(reminderUseCase.createForTelegram(new CreateTelegramReminderCommand(
                request.telegramId(), request.description(), request.reminderDate(),
                request.daysBefore(), request.recurringDescription()
        )));
    }

    @PostMapping("/pending/claim")
    public List<PendingReminderResponse> findPending() {
        return reminderUseCase.claimPending(LocalDate.now()).stream()
                .map(reminder -> new PendingReminderResponse(
                        reminder.getId(), reminder.getTelegramId(), reminder.getDescription(), reminder.getReminderDate()
                )).toList();
    }

    @PatchMapping("/{id}/sent")
    public void markSent(@PathVariable Long id) {
        reminderUseCase.markSent(id);
    }

    @PatchMapping("/{id}/release")
    public void release(@PathVariable Long id) {
        reminderUseCase.releaseClaim(id);
    }
}
