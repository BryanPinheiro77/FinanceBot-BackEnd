package com.financebot.reminder.controller;

import com.financebot.reminder.application.usecase.ReminderUseCase;
import com.financebot.reminder.domain.Reminder;
import com.financebot.reminder.dto.request.CreateReminderRequest;
import com.financebot.reminder.dto.request.UpdateReminderRequest;
import com.financebot.reminder.dto.response.ReminderResponse;
import com.financebot.reminder.mapper.ReminderMapper;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderControllerTest {
    @Mock ReminderUseCase reminderUseCase;
    @Mock ReminderMapper reminderMapper;
    @Mock AuthenticatedUserResolver authenticatedUserResolver;
    @Mock Authentication authentication;
    @InjectMocks ReminderController controller;

    @Test
    void shouldDelegateReminderCrudOperations() {
        User user = new User();
        user.setId(10L);
        Reminder reminder = new Reminder();
        ReminderResponse response = mock(ReminderResponse.class);
        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(reminderUseCase.create(any())).thenReturn(reminder);
        when(reminderUseCase.findAll(10L)).thenReturn(List.of(reminder));
        when(reminderUseCase.update(eq(3L), any())).thenReturn(reminder);
        when(reminderMapper.toResponse(reminder)).thenReturn(response);

        assertThat(controller.create(new CreateReminderRequest("Aluguel", LocalDate.now(), 2, null), authentication))
                .isEqualTo(response);
        assertThat(controller.findAll(authentication)).containsExactly(response);
        assertThat(controller.update(3L,
                new UpdateReminderRequest("Aluguel", LocalDate.now(), 2, true), authentication))
                .isEqualTo(response);
        controller.delete(3L, authentication);

        verify(reminderUseCase).create(any());
        verify(reminderUseCase).findAll(10L);
        verify(reminderUseCase).update(eq(3L), any());
        verify(reminderUseCase).delete(3L, 10L);
    }
}
