package com.financebot.telegram.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.telegram.application.command.CreateTelegramExistingInstallmentCommand;
import com.financebot.telegram.application.command.CreateTelegramInstallmentCommand;
import com.financebot.telegram.application.command.CreateTelegramTransactionCommand;
import com.financebot.telegram.service.TelegramAccountResolverService;
import com.financebot.telegram.service.TelegramCategoryResolverService;
import com.financebot.transaction.application.command.CreateExistingInstallmentTransactionCommand;
import com.financebot.transaction.application.command.CreateInstallmentTransactionCommand;
import com.financebot.transaction.application.command.CreateTransactionCommand;
import com.financebot.transaction.application.usecase.CreateExistingInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelegramTransactionApplicationUseCase {

    private final UserRepository userRepository;
    private final TelegramAccountResolverService accountResolver;
    private final TelegramCategoryResolverService categoryResolver;
    private final CreateTransactionUseCase createTransactionUseCase;
    private final CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;
    private final CreateExistingInstallmentTransactionUseCase createExistingInstallmentTransactionUseCase;

    @Transactional
    public void create(CreateTelegramTransactionCommand command) {
        User user = findUser(command.telegramId());
        TransactionType type = command.type();
        Account account = accountResolver.resolve(user, command.accountName());
        Category category = resolveCategory(user, type, command.categoryName(), command.description());

        createTransactionUseCase.execute(new CreateTransactionCommand(
                command.amount(), command.description(), command.date(), type,
                SourceType.BOT_TEXT, account.getId(), category.getId(), user
        ));
    }

    @Transactional
    public void createInstallment(CreateTelegramInstallmentCommand command) {
        User user = findUser(command.telegramId());
        Account account = accountResolver.resolve(user, command.accountName());
        Category category = resolveCategory(user, TransactionType.EXPENSE, command.categoryName(), command.description());

        createInstallmentTransactionUseCase.execute(new CreateInstallmentTransactionCommand(
                command.totalAmount(), command.description(), command.firstInstallmentDate(),
                TransactionType.EXPENSE, SourceType.BOT_TEXT, account.getId(), category.getId(),
                command.totalInstallments(), user
        ));
    }

    @Transactional
    public void createExistingInstallment(CreateTelegramExistingInstallmentCommand command) {
        User user = findUser(command.telegramId());
        Account account = accountResolver.resolve(user, command.accountName());
        Category category = resolveCategory(user, TransactionType.EXPENSE, command.categoryName(), command.description());

        createExistingInstallmentTransactionUseCase.execute(new CreateExistingInstallmentTransactionCommand(
                command.totalAmount(), command.monthlyAmount(), command.description(),
                command.firstRemainingInstallmentDate(), TransactionType.EXPENSE, SourceType.BOT_TEXT,
                account.getId(), category.getId(), command.totalInstallments(),
                command.firstRemainingInstallmentNumber(), user
        ));
    }

    private User findUser(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(TelegramUserNotFoundException::new);
    }

    private Category resolveCategory(User user, TransactionType type, String categoryName, String description) {
        if (categoryName != null && !categoryName.isBlank()) {
            return categoryResolver.resolveExplicitCategory(user, type, categoryName);
        }
        return categoryResolver.resolveCategory(user, type, description);
    }
}
