package com.financebot.alert.adapter;

import com.financebot.alert.service.FinancialNotificationService;
import com.financebot.alert.application.NotificationDeliveryClaim;
import com.financebot.alert.domain.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TelegramFinancialNotificationControllerTest {
    @Mock FinancialNotificationService service;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TelegramFinancialNotificationController(service)).build();
    }

    @Test void duplicateOrCancelledClaimReturnsNoContent() throws Exception {
        mvc.perform(post("/telegram/financial-notifications/id/claim")).andExpect(status().isNoContent());
    }

    @Test void eligibleClaimReturnsAuthoritativeChatAndToken() throws Exception {
        when(service.claim("id")).thenReturn(new NotificationDeliveryClaim("token", 123L, "Título", "Corpo"));
        mvc.perform(post("/telegram/financial-notifications/id/claim"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.telegramId").value(123));
    }

    @Test void missingPreferenceFieldsAreRejected() throws Exception {
        mvc.perform(patch("/telegram/financial-notifications/preferences?telegramId=123")
                        .contentType("application/json").content("{\"alerts\":false}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void missingTokenCannotAcknowledgeDelivery() throws Exception {
        mvc.perform(patch("/telegram/financial-notifications/id/delivery").contentType("application/json")
                        .content("{\"outcome\":\"SENT\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void deliveryResultKeepsReservationToken() throws Exception {
        mvc.perform(patch("/telegram/financial-notifications/id/delivery").contentType("application/json")
                        .content("{\"token\":\"token\",\"outcome\":\"SENT\"}"))
                .andExpect(status().isOk());
        verify(service).complete("id", "token", NotificationStatus.SENT);
    }
}
