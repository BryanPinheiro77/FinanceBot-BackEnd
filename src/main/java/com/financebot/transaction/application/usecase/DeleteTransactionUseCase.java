package com.financebot.transaction.application.usecase;

import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeleteTransactionUseCase {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found";

    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional
    public void execute(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);

        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE));

        transactionRepository.delete(transaction);
    }
}