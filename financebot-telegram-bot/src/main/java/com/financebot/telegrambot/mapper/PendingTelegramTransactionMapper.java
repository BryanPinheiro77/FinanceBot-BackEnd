package com.financebot.telegrambot.mapper;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PendingTelegramTransactionMapper {

    public PendingTelegramTransaction fromParsedMessage(ParsedTelegramMessage parsedMessage) {
        return new PendingTelegramTransaction(
                parsedMessage.intentType(),
                resolveAmount(parsedMessage),
                parsedMessage.monthlyAmount(),
                parsedMessage.description(),
                parsedMessage.date(),
                parsedMessage.categoryName(),
                parsedMessage.accountName(),
                parsedMessage.totalInstallments(),
                parsedMessage.firstRemainingInstallmentNumber(),
                parsedMessage.originalMessage()
        );
    }

    private BigDecimal resolveAmount(ParsedTelegramMessage parsedMessage) {
        if (parsedMessage.totalAmount() != null) {
            return parsedMessage.totalAmount();
        }

        return parsedMessage.amount();
    }
}