package com.financebot.transaction.application.usecase;

import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.FindTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindTransactionByIdUseCaseTest {

    @Mock
    private FindTransactionPort findTransactionPort;

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FindTransactionByIdUseCase findTransactionByIdUseCase;

    @Test
    @DisplayName("deve buscar transação por id com sucesso")
    void shouldFindByIdSuccessfully() {
        User user = buildUser(1L, "bryan@email.com");
        Transaction transaction = new Transaction();
        TransactionResponse response = mock(TransactionResponse.class);

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(findTransactionPort.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponse result = findTransactionByIdUseCase.execute(99L, authentication);

        assertThat(result).isEqualTo(response);
        verify(transactionMapper).toResponse(transaction);
    }

    @Test
    @DisplayName("deve lançar erro quando transação não for encontrada para o usuário")
    void shouldThrowWhenTransactionIsNotFound() {
        User user = buildUser(1L, "bryan@email.com");

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(findTransactionPort.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> findTransactionByIdUseCase.execute(99L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Transaction not found");

        verifyNoInteractions(transactionMapper);
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}