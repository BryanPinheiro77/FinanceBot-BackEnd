package com.financebot.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import com.financebot.telegram.exception.TelegramUserNotFoundException;

import java.lang.reflect.Method;
import java.util.stream.Stream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleNotFoundExceptions() {
        ProblemDetail entity = handler.handleEntityNotFound(new EntityNotFoundException("Conta não encontrada"));
        ProblemDetail telegram = handler.handleTelegramUserNotFound(new TelegramUserNotFoundException());

        assertThat(entity.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(entity.getTitle()).isEqualTo("Resource not found");
        assertThat(entity.getDetail()).isEqualTo("Conta não encontrada");
        assertThat(telegram.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(telegram.getTitle()).isEqualTo("Telegram user not found");
    }

    @Test
    void shouldHandleBadRequestAndUnauthorizedExceptions() {
        ProblemDetail illegal = handler.handleIllegalArgument(new IllegalArgumentException("inválido"));
        ProblemDetail validation = handler.handleValidation(new ValidationException("falha"));
        ProblemDetail violation = handler.handleConstraintViolation(new ConstraintViolationException("restrição", Set.of()));
        ProblemDetail unauthorized = handler.handleUnauthorized(new UnauthorizedException("token ausente"));

        assertThat(illegal.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(illegal.getDetail()).isEqualTo("inválido");
        assertThat(validation.getDetail()).isEqualTo("falha");
        assertThat(violation.getDetail()).isEqualTo("restrição");
        assertThat(unauthorized.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(unauthorized.getTitle()).isEqualTo("Unauthorized");
    }

    @ParameterizedTest
    @MethodSource("fieldErrorScenarios")
    @DisplayName("deve formatar erro de campo usando mensagem padrao ou nome do campo")
    void shouldFormatFieldErrorUsingDefaultMessageOrFieldName(
            String defaultMessage,
            String expectedDetail
    ) throws Exception {
        MethodArgumentNotValidException exception = buildValidationException(
                new FieldError("request", "amount", defaultMessage)
        );

        ProblemDetail problemDetail = handler.handleMethodArgumentNotValid(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Invalid request");
        assertThat(problemDetail.getDetail()).isEqualTo(expectedDetail);
    }

    @Test
    @DisplayName("deve juntar multiplos erros de validacao com ponto e virgula")
    void shouldJoinMultipleValidationErrorsWithSemicolon() throws Exception {
        MethodArgumentNotValidException exception = buildValidationException(
                new FieldError("request", "amount", "Valor deve ser maior que zero"),
                new FieldError("request", "description", "Descrição é obrigatória")
        );

        ProblemDetail problemDetail = handler.handleMethodArgumentNotValid(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Invalid request");
        assertThat(problemDetail.getDetail())
                .isEqualTo("Valor deve ser maior que zero; Descrição é obrigatória");
    }

    @Test
    @DisplayName("deve retornar mensagem generica quando nao houver erros de campo")
    void shouldReturnGenericMessageWhenThereAreNoFieldErrors() throws Exception {
        MethodArgumentNotValidException exception = buildValidationException();

        ProblemDetail problemDetail = handler.handleMethodArgumentNotValid(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Invalid request");
        assertThat(problemDetail.getDetail()).isEqualTo("Dados inválidos");
    }

    private static Stream<Arguments> fieldErrorScenarios() {
        return Stream.of(
                Arguments.of("Valor deve ser maior que zero", "Valor deve ser maior que zero"),
                Arguments.of(null, "amount"),
                Arguments.of("   ", "amount")
        );
    }

    private MethodArgumentNotValidException buildValidationException(FieldError... fieldErrors) throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        for (FieldError fieldError : fieldErrors) {
            bindingResult.addError(fieldError);
        }

        Method method = ValidationControllerFixture.class.getDeclaredMethod("validate", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        return new MethodArgumentNotValidException(methodParameter, bindingResult);
    }

    private static class ValidationControllerFixture {

        public void validate(String value) {
            throw new UnsupportedOperationException("Test fixture method should not be executed: " + value);
        }
    }
}
