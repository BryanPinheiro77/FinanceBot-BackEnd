package com.financebot.telegrambot.support;

import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TelegramBotErrorMapper {

    private final TelegramMessageFormatter telegramMessageFormatter;

    public String mapDefaultBotErrors(RestClientResponseException exception) {
        return telegramMessageFormatter.formatDefaultBotErrorMessage(
                exception.getStatusCode().value()
        );
    }
}