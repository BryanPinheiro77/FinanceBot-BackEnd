package com.financebot.transaction.mapper;

import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.dto.CreateTransactionRequest;
import com.financebot.transaction.dto.TransactionResponse;
import com.financebot.transaction.dto.UpdateTransactionRequest;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public Transaction toEntity(CreateTransactionRequest dto) {
        Transaction transaction = new Transaction();
        transaction.setAmount(dto.amount());
        transaction.setDescription(dto.description().trim());
        transaction.setDate(dto.date());
        transaction.setType(dto.type());
        transaction.setSourceType(dto.sourceType());
        return transaction;
    }

    public void updateEntity(UpdateTransactionRequest dto, Transaction transaction) {
        transaction.setAmount(dto.amount());
        transaction.setDescription(dto.description().trim());
        transaction.setDate(dto.date());
        transaction.setType(dto.type());
        transaction.setSourceType(dto.sourceType());
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