package com.financebot.transaction.application.usecase;

import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.FindTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FindTransactionByIdUseCase {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found";

    private final FindTransactionPort findTransactionPort;
    private final TransactionMapper transactionMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional(readOnly = true)
    public TransactionResponse execute(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);

        Transaction transaction = findTransactionPort.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE));

        return transactionMapper.toResponse(transaction);
    }
}