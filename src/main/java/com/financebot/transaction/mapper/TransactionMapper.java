package com.financebot.transaction.mapper;

import com.financebot.transaction.application.command.CreateInstallmentTransactionCommand;
import com.financebot.transaction.application.command.CreateTransactionCommand;
import com.financebot.transaction.application.command.UpdateTransactionCommand;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.domain.Transaction;
import com.financebot.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public CreateTransactionCommand toCommand(CreateTransactionRequest request, User user) {
        return new CreateTransactionCommand(
                request.amount(),
                request.description(),
                request.date(),
                request.type(),
                request.sourceType(),
                request.accountId(),
                request.categoryId(),
                user
        );
    }

    public Transaction toEntity(CreateTransactionCommand command) {
        Transaction transaction = new Transaction();
        transaction.setAmount(command.amount());
        transaction.setDescription(command.description().trim());
        transaction.setDate(command.date());
        transaction.setType(command.type());
        transaction.setSourceType(command.sourceType());
        return transaction;
    }

    public CreateInstallmentTransactionCommand toCommand(
            CreateInstallmentTransactionRequest request,
            User user
    ) {
        return new CreateInstallmentTransactionCommand(
                request.totalAmount(),
                request.description(),
                request.firstInstallmentDate(),
                request.type(),
                request.sourceType(),
                request.accountId(),
                request.categoryId(),
                request.totalInstallments(),
                user
        );
    }

    public UpdateTransactionCommand toCommand(
            Long transactionId,
            UpdateTransactionRequest request,
            User user
    ) {
        return new UpdateTransactionCommand(
                transactionId,
                request.amount(),
                request.description(),
                request.date(),
                request.type(),
                request.sourceType(),
                request.accountId(),
                request.categoryId(),
                user
        );
    }

    public void updateEntity(UpdateTransactionCommand command, Transaction transaction) {
        transaction.setAmount(command.amount());
        transaction.setDescription(command.description().trim());
        transaction.setDate(command.date());
        transaction.setType(command.type());
        transaction.setSourceType(command.sourceType());
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getDate(),
                transaction.getType(),
                transaction.getSourceType(),
                transaction.getInstallment(),
                transaction.getInstallmentNumber(),
                transaction.getTotalInstallments(),
                transaction.getInstallmentGroupId(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getCreatedAt()
        );
    }
}