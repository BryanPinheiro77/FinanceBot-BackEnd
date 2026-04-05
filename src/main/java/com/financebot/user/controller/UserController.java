package com.financebot.user.controller;

import com.financebot.user.dto.request.TelegramLinkConfirmRequest;
import com.financebot.user.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.user.dto.response.CurrentUserResponse;
import com.financebot.user.dto.response.TelegramLinkCodeResponse;
import com.financebot.user.dto.response.TelegramLinkConfirmResponse;
import com.financebot.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public CurrentUserResponse getMe(Authentication authentication) {
        return userService.getMe(authentication);
    }

    @PatchMapping("/me/monthly-base-income")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMonthlyBaseIncome(
            @RequestBody @Valid UpdateMonthlyBaseIncomeRequest request,
            Authentication authentication
    ) {
        userService.updateMonthlyBaseIncome(request, authentication);
    }

    @PostMapping("/me/telegram-link-code")
    public TelegramLinkCodeResponse generateTelegramLinkCode(Authentication authentication) {
        return userService.generateTelegramLinkCode(authentication);
    }

    @PostMapping("/telegram/confirm-link")
    public TelegramLinkConfirmResponse confirmTelegramLink(
            @RequestBody @Valid TelegramLinkConfirmRequest request
    ) {
        return userService.confirmTelegramLink(request);
    }

    @DeleteMapping("/me/telegram-link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnectTelegram(Authentication authentication) {
        userService.disconnectTelegram(authentication);
    }
}