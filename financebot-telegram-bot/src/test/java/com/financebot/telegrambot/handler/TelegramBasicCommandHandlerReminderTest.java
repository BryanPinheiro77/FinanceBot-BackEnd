package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.formatter.TelegramAccountMessageFormatter;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramBasicCommandHandlerReminderTest {

    @Mock
    private FinanceBotApiClient financeBotApiClient;
    @Mock
    private TelegramPendingConfirmationService pendingConfirmationService;
    @Mock
    private TelegramPendingQueryService pendingQueryService;
    @Mock
    private TelegramBotErrorMapper errorMapper;

    private TelegramBasicCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TelegramBasicCommandHandler(
                financeBotApiClient,
                pendingConfirmationService,
                pendingQueryService,
                new TelegramAccountMessageFormatter(),
                errorMapper
        );
    }

    @Test
    void createsReminderWithoutDaysBefore() {
        String response = handler.handleReminder("/lembrete 2026-10-10 pagar aluguel", 123L);

        assertThat(response).contains("2026-10-10");
        verify(financeBotApiClient).createReminder(123L, "pagar aluguel", LocalDate.of(2026, 10, 10), 0);
    }

    @Test
    void createsReminderWithDaysBefore() {
        String response = handler.handleReminder("/lembrete 2026-10-10 2 pagar aluguel", 123L);

        assertThat(response).contains("2026-10-08");
        verify(financeBotApiClient).createReminder(123L, "pagar aluguel", LocalDate.of(2026, 10, 8), 0);
    }
}
