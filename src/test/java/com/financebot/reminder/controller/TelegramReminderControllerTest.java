package com.financebot.reminder.controller;

import com.financebot.reminder.application.command.CreateTelegramReminderCommand;
import com.financebot.reminder.application.usecase.ReminderUseCase;
import com.financebot.reminder.domain.Reminder;
import com.financebot.reminder.mapper.ReminderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class TelegramReminderControllerTest {
    @Mock private ReminderUseCase reminderUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new TelegramReminderController(reminderUseCase, new ReminderMapper())).build();
    }

    @Test
    void createsReminderLinkedToRecurrence() throws Exception {
        Reminder reminder = reminder(1L, "internet", LocalDate.of(2026, 10, 8));
        reminder.setRecurringTransactionId(4L);
        when(reminderUseCase.createForTelegram(any())).thenReturn(reminder);

        mockMvc.perform(post("/telegram/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramId":123,"description":"internet","daysBefore":2,
                                 "recurringDescription":"internet"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderDate").value("2026-10-08"))
                .andExpect(jsonPath("$.recurringTransactionId").value(4));

        ArgumentCaptor<CreateTelegramReminderCommand> command =
                ArgumentCaptor.forClass(CreateTelegramReminderCommand.class);
        verify(reminderUseCase).createForTelegram(command.capture());
        assertThat(command.getValue().reminderDate()).isNull();
        assertThat(command.getValue().daysBefore()).isEqualTo(2);
    }

    @Test
    void claimsPendingReminders() throws Exception {
        when(reminderUseCase.claimPending(any())).thenReturn(List.of(
                reminder(1L, "Pagar aluguel", LocalDate.of(2026, 10, 8))
        ));

        mockMvc.perform(post("/telegram/reminders/pending/claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].telegramId").value(123))
                .andExpect(jsonPath("$[0].description").value("Pagar aluguel"));
    }

    @Test
    void confirmsAndReleasesDeliveries() throws Exception {
        mockMvc.perform(patch("/telegram/reminders/1/sent")).andExpect(status().isOk());
        mockMvc.perform(patch("/telegram/reminders/2/release")).andExpect(status().isOk());

        verify(reminderUseCase).markSent(1L);
        verify(reminderUseCase).releaseClaim(2L);
    }

    @Test
    void checksWhetherQueuedDeliveryIsStillCurrent() throws Exception {
        LocalDate reminderDate = LocalDate.of(2026, 10, 8);
        when(reminderUseCase.isCurrentDelivery(1L, reminderDate)).thenReturn(true);

        mockMvc.perform(get("/telegram/reminders/1/deliverable")
                        .queryParam("reminderDate", "2026-10-08"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("true"));
    }

    private Reminder reminder(Long id, String description, LocalDate date) {
        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setTelegramId(123L);
        reminder.setDescription(description);
        reminder.setReminderDate(date);
        return reminder;
    }
}
