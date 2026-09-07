package com.financebot.reminder.controller;

import com.financebot.reminder.dto.response.PendingReminderResponse;
import com.financebot.reminder.dto.request.CreateTelegramReminderRequest;
import com.financebot.reminder.dto.response.ReminderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.financebot.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/telegram/reminders")
@RequiredArgsConstructor
public class TelegramReminderController {

    private final ReminderService reminderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(@RequestBody @Valid CreateTelegramReminderRequest request) {
        return reminderService.createForTelegram(request);
    }

    @GetMapping("/pending")
    public List<PendingReminderResponse> findPending() {
        return reminderService.findPendingForTelegram(LocalDate.now());
    }

    @PatchMapping("/{id}/sent")
    public void markSent(@PathVariable Long id) {
        reminderService.markSent(id);
    }
}
