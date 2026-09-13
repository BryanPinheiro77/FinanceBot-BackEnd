package com.financebot.reminder.adapter.out.persistence;

import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(com.financebot.security.config.DataEncryptionConfig.class)
class SpringDataReminderRepositoryTest {
    @Autowired private SpringDataReminderRepository reminderRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void findsOnlyDueAndAvailableRemindersWithinBatchLimit() {
        LocalDate today = LocalDate.of(2026, 10, 8);
        LocalDateTime staleBefore = LocalDateTime.of(2026, 10, 8, 9, 55);
        User user = saveUser(123L);
        User userWithoutTelegram = saveUser(null);

        ReminderJpaEntity due = saveReminder(user, "Aluguel", today, null);
        ReminderJpaEntity staleClaim = saveReminder(
                user, "Internet", today.minusDays(1), staleBefore.minusSeconds(1));
        saveReminder(user, "Reserva recente", today, staleBefore.plusSeconds(1));
        saveReminder(user, "Futuro", today.plusDays(1), null);
        saveReminder(userWithoutTelegram, "Sem Telegram", today, null);

        List<ReminderJpaEntity> result = reminderRepository.findPendingForClaim(
                today, staleBefore, PageRequest.of(0, 2));

        assertThat(result).extracting(ReminderJpaEntity::getId)
                .containsExactly(staleClaim.getId(), due.getId());
    }

    private User saveUser(Long telegramId) {
        User user = new User();
        user.setName("Usuário");
        user.setEmail("user-" + (telegramId == null ? "without-telegram" : telegramId) + "@example.com");
        user.setPassword("test-password");
        user.setMonthlyBaseIncome(BigDecimal.ZERO);
        user.setTelegramId(telegramId);
        return userRepository.save(user);
    }

    private ReminderJpaEntity saveReminder(
            User user, String description, LocalDate date, LocalDateTime claimedAt) {
        ReminderJpaEntity reminder = new ReminderJpaEntity();
        reminder.setUser(user);
        reminder.setDescription(description);
        reminder.setReminderDate(date);
        reminder.setActive(true);
        reminder.setClaimedAt(claimedAt);
        return reminderRepository.saveAndFlush(reminder);
    }
}
