package com.financebot.transaction.mapper;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.dto.request.CreateExistingInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapper();
    private final User user = new User();

    @Test
    void shouldMapCreateTransactionRequestToCommandAndEntity() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("12.50"), "  café  ", LocalDate.of(2026, 9, 1),
                TransactionType.EXPENSE, SourceType.WEB, 1L, 2L);

        var command = mapper.toCommand(request, user);
        Transaction transaction = mapper.toEntity(command);

        assertThat(command.user()).isSameAs(user);
        assertThat(command.accountId()).isEqualTo(1L);
        assertThat(transaction.getDescription()).isEqualTo("café");
        assertThat(transaction.getAmount()).isEqualByComparingTo("12.50");
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void shouldMapInstallmentRequestsToCommands() {
        CreateInstallmentTransactionRequest installment = new CreateInstallmentTransactionRequest(
                new BigDecimal("100"), "compra", LocalDate.of(2026, 9, 2),
                TransactionType.EXPENSE, SourceType.BOT_TEXT, 1L, 2L, 3);
        CreateExistingInstallmentTransactionRequest existing = new CreateExistingInstallmentTransactionRequest(
                null, new BigDecimal("25"), "compra", LocalDate.of(2026, 9, 2),
                TransactionType.EXPENSE, SourceType.WEB, 1L, 2L, 4, 2);

        var installmentCommand = mapper.toCommand(installment, user);
        var existingCommand = mapper.toCommand(existing, user);

        assertThat(installmentCommand.totalInstallments()).isEqualTo(3);
        assertThat(existingCommand.monthlyAmount()).isEqualByComparingTo("25");
        assertThat(existingCommand.firstRemainingInstallmentNumber()).isEqualTo(2);
    }

    @Test
    void shouldMapUpdateCommandAndMutateEntity() {
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                new BigDecimal("20"), "  atualizado ", LocalDate.of(2026, 9, 3),
                TransactionType.INCOME, SourceType.BOT_AUDIO, 3L, 4L);

        var command = mapper.toCommand(9L, request, user);
        Transaction transaction = new Transaction();
        mapper.updateEntity(command, transaction);

        assertThat(command.transactionId()).isEqualTo(9L);
        assertThat(transaction.getDescription()).isEqualTo("atualizado");
        assertThat(transaction.getSourceType()).isEqualTo(SourceType.BOT_AUDIO);
    }

    @Test
    void shouldMapTransactionResponseWithAccountAndCategoryData() {
        Transaction transaction = new Transaction();
        transaction.setId(9L);
        transaction.setAmount(new BigDecimal("20"));
        transaction.setDescription("compra");
        transaction.setDate(LocalDate.of(2026, 9, 3));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setSourceType(SourceType.WEB);
        transaction.setInstallment(true);
        transaction.setInstallmentNumber(2);
        transaction.setTotalInstallments(3);
        transaction.setInstallmentGroupId("group");
        Account account = new Account();
        account.setId(3L);
        account.setName("Conta");
        Category category = new Category();
        category.setId(4L);
        category.setName("Casa");
        transaction.setAccount(account);
        transaction.setCategory(category);

        var response = mapper.toResponse(transaction);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.accountId()).isEqualTo(3L);
        assertThat(response.accountName()).isEqualTo("Conta");
        assertThat(response.categoryId()).isEqualTo(4L);
        assertThat(response.installmentGroupId()).isEqualTo("group");
    }
}
