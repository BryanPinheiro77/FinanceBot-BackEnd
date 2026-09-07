package com.financebot.reminder.controller;

import com.financebot.reminder.dto.request.CreateReminderRequest;
import com.financebot.reminder.dto.request.UpdateReminderRequest;
import com.financebot.reminder.dto.response.ReminderResponse;
import com.financebot.reminder.application.usecase.ReminderUseCase;
import com.financebot.reminder.mapper.ReminderMapper;
import com.financebot.user.service.AuthenticatedUserResolver;
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

    private final ReminderUseCase reminderUseCase;
    private final ReminderMapper reminderMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(@RequestBody @Valid CreateReminderRequest request, Authentication authentication) {
        return reminderMapper.toResponse(reminderUseCase.create(
                request, authenticatedUserResolver.resolve(authentication).getId()
        ));
    }

    @GetMapping
    public List<ReminderResponse> findAll(Authentication authentication) {
        Long userId = authenticatedUserResolver.resolve(authentication).getId();
        return reminderUseCase.findAll(userId).stream().map(reminderMapper::toResponse).toList();
    }

    @PutMapping("/{id}")
    public ReminderResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateReminderRequest request,
            Authentication authentication
    ) {
        return reminderMapper.toResponse(reminderUseCase.update(
                id, request, authenticatedUserResolver.resolve(authentication).getId()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        reminderUseCase.delete(id, authenticatedUserResolver.resolve(authentication).getId());
    }
}
