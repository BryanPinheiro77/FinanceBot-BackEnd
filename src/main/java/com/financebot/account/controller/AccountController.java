package com.financebot.account.controller;

import com.financebot.account.dto.AccountResponse;
import com.financebot.account.dto.CreateAccountRequest;
import com.financebot.account.dto.UpdateAccountRequest;
import com.financebot.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(
            @RequestBody @Valid CreateAccountRequest request,
            Authentication authentication
    ) {
        return accountService.create(request, authentication);
    }

    @GetMapping
    public List<AccountResponse> findAll(Authentication authentication) {
        return accountService.findAll(authentication);
    }

    @GetMapping("/{id}")
    public AccountResponse findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return accountService.findById(id, authentication);
    }

    @PutMapping("/{id}")
    public AccountResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateAccountRequest request,
            Authentication authentication
    ) {
        return accountService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        accountService.delete(id, authentication);
    }
}