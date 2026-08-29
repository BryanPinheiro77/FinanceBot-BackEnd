package com.financebot.telegram.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.telegram.application.command.CreateTelegramTransactionCommand;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.telegram.service.TelegramAccountResolverService;
import com.financebot.telegram.service.TelegramCategoryResolverService;
import com.financebot.transaction.application.usecase.CreateExistingInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramTransactionApplicationUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TelegramAccountResolverService accountResolver;

    @Mock
    private TelegramCategoryResolverService categoryResolver;

    @Mock
    private CreateTransactionUseCase createTransactionUseCase;

    @Mock
    private CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;

    @Mock
    private CreateExistingInstallmentTransactionUseCase createExistingInstallmentTransactionUseCase;

    @InjectMocks
    private TelegramTransactionApplicationUseCase useCase;

    @Test
    void shouldDelegateTransactionCreationWithInternalCommand() {
        User user = new User();
        Account account = new Account();
        account.setId(10L);
        Category category = new Category();
        category.setId(20L);

        when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
        when(accountResolver.resolve(user, "Carteira")).thenReturn(account);
        when(categoryResolver.resolveCategory(user, TransactionType.EXPENSE, "mercado")).thenReturn(category);

        useCase.create(new CreateTelegramTransactionCommand(
                123L, new BigDecimal("50.00"), "mercado", LocalDate.of(2026, 8, 29),
                TransactionType.EXPENSE, null, "Carteira"
        ));

        verify(createTransactionUseCase).execute(any());
    }

    @Test
    void shouldRejectUnknownTelegramUserBeforeDelegating() {
        when(userRepository.findByTelegramId(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.create(new CreateTelegramTransactionCommand(
                123L, BigDecimal.TEN, "mercado", LocalDate.of(2026, 8, 29),
                TransactionType.EXPENSE, null, null
        ))).isInstanceOf(TelegramUserNotFoundException.class);
    }
}
