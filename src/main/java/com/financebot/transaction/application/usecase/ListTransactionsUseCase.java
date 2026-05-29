package com.financebot.transaction.application.usecase;

import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.FindTransactionPort;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ListTransactionsUseCase {

    private static final String INVALID_PERIOD_MESSAGE = "Start date cannot be after end date";

    private final FindTransactionPort findTransactionPort;
    private final TransactionMapper transactionMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> execute(
            TransactionFilter filter,
            Authentication authentication,
            Pageable pageable
    ) {
        User user = authenticatedUserResolver.resolve(authentication);

        validatePeriod(filter.startDate(), filter.endDate());

        return findTransactionPort.findAllByFilter(user.getId(), filter, pageable)
                .map(transactionMapper::toResponse);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(INVALID_PERIOD_MESSAGE);
        }
    }
}

