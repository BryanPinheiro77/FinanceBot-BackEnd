package com.financebot.reminder.controller;

import com.financebot.reminder.dto.request.CreateReminderRequest;
import com.financebot.reminder.dto.request.UpdateReminderRequest;
import com.financebot.reminder.dto.response.ReminderResponse;
import com.financebot.reminder.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(@RequestBody @Valid CreateReminderRequest request, Authentication authentication) {
        return reminderService.create(request, authentication);
    }

    @GetMapping
    public List<ReminderResponse> findAll(Authentication authentication) {
        return reminderService.findAll(authentication);
    }

    @PutMapping("/{id}")
    public ReminderResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateReminderRequest request,
            Authentication authentication
    ) {
        return reminderService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        reminderService.delete(id, authentication);
    }
}
