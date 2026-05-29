package com.financebot.transaction.application.usecase;

import com.financebot.transaction.application.port.out.DeleteTransactionPort;
import com.financebot.transaction.application.port.out.FindTransactionPort;
import com.financebot.transaction.domain.Transaction;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteTransactionUseCaseTest {

    @Mock
    private FindTransactionPort findTransactionPort;

    @Mock
    private DeleteTransactionPort deleteTransactionPort;

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DeleteTransactionUseCase deleteTransactionUseCase;

    @Test
    @DisplayName("deve deletar transação com sucesso")
    void shouldDeleteSuccessfully() {
        User user = buildUser(1L, "bryan@email.com");
        Transaction transaction = new Transaction();

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(findTransactionPort.findByIdAndUserId(55L, 1L)).thenReturn(Optional.of(transaction));

        deleteTransactionUseCase.execute(55L, authentication);

        verify(deleteTransactionPort).delete(transaction);
    }

    @Test
    @DisplayName("deve lançar erro quando transação a deletar não existir para o usuário")
    void shouldThrowWhenTransactionToDeleteDoesNotExist() {
        User user = buildUser(1L, "bryan@email.com");

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(findTransactionPort.findByIdAndUserId(55L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteTransactionUseCase.execute(55L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Transaction not found");

        verify(deleteTransactionPort, never()).delete(any(Transaction.class));
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}