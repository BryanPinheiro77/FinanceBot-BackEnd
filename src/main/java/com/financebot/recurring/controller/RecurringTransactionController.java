package com.financebot.recurring.controller;

import com.financebot.recurring.dto.request.CreateRecurringTransactionRequest;
import com.financebot.recurring.dto.request.UpdateRecurringTransactionRequest;
import com.financebot.recurring.dto.response.RecurringTransactionResponse;
import com.financebot.recurring.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringTransactionResponse create(
            @RequestBody @Valid CreateRecurringTransactionRequest request,
            Authentication authentication
    ) {
        return recurringTransactionService.create(request, authentication);
    }

    @GetMapping
    public List<RecurringTransactionResponse> findAll(Authentication authentication) {
        return recurringTransactionService.findAll(authentication);
    }

    @GetMapping("/{id}")
    public RecurringTransactionResponse findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return recurringTransactionService.findById(id, authentication);
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateRecurringTransactionRequest request,
            Authentication authentication
    ) {
        return recurringTransactionService.update(id, request, authentication);
    }

    @PatchMapping("/{id}/activate")
    public RecurringTransactionResponse activate(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return recurringTransactionService.activate(id, authentication);
    }

    @PatchMapping("/{id}/deactivate")
    public RecurringTransactionResponse deactivate(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return recurringTransactionService.deactivate(id, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        recurringTransactionService.delete(id, authentication);
    }
}