package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.dto.request.InstallmentPurchaseCapacityRequest;
import com.financebot.telegrambot.dto.request.TelegramInstallmentCountRequest;
import com.financebot.telegrambot.dto.request.TelegramTransactionSummaryRequest;
import com.financebot.telegrambot.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.telegrambot.dto.response.MonthlyAmountSummaryResponse;
import com.financebot.telegrambot.dto.response.TelegramActiveInstallmentSummaryResponse;
import com.financebot.telegrambot.dto.response.TelegramActiveInstallmentsResponse;
import com.financebot.telegrambot.dto.response.TelegramInstallmentCountResponse;
import com.financebot.telegrambot.dto.response.TelegramTransactionSummaryResponse;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.service.TelegramQueryContextService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramFinancialQueryHandler {

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramPendingQueryService telegramPendingQueryService;
    private final TelegramQueryContextService telegramQueryContextService;
    private final TelegramMessageFormatter telegramMessageFormatter;
    private final TelegramBotErrorMapper telegramBotErrorMapper;
    private final TelegramBasicCommandHandler telegramBasicCommandHandler;

    public String handleQuery(ParsedTelegramMessage parsedMessage, Long telegramId) {
        try {
            String resultMessage = switch (parsedMessage.intentType()) {
                case QUERY_MONTH_EXPENSE_TOTAL -> {
                    MonthlyAmountSummaryResponse response =
                            financeBotApiClient.getCurrentMonthExpenseSummary(telegramId);

                    yield telegramMessageFormatter.formatMonthExpenseSummary(response.totalAmount());
                }

                case QUERY_MONTH_INCOME_TOTAL -> {
                    MonthlyAmountSummaryResponse response =
                            financeBotApiClient.getCurrentMonthIncomeSummary(telegramId);

                    yield telegramMessageFormatter.formatMonthIncomeSummary(response.totalAmount());
                }

                case QUERY_MONTH_ANALYSIS -> telegramBasicCommandHandler.handleAnalysis(telegramId);

                case QUERY_TRANSACTION_TOTAL -> {
                    String originalMessage = parsedMessage.originalMessage().toLowerCase();

                    String type = originalMessage.contains("recebi")
                            || originalMessage.contains("entrou")
                            ? "INCOME"
                            : "EXPENSE";

                    TelegramTransactionSummaryResponse response = financeBotApiClient.getTransactionSummary(
                            new TelegramTransactionSummaryRequest(
                                    telegramId,
                                    type,
                                    parsedMessage.categoryName(),
                                    parsedMessage.accountName(),
                                    parsedMessage.startDate(),
                                    parsedMessage.endDate()
                            )
                    );

                    String label = "EXPENSE".equals(type) ? "gasto" : "recebido";

                    StringBuilder complemento = new StringBuilder();

                    if (response.categoryName() != null) {
                        complemento.append(" em ").append(response.categoryName());
                    }

                    if (response.accountName() != null) {
                        complemento.append(" na conta ").append(response.accountName());
                    }

                    yield telegramMessageFormatter.formatTransactionSummary(
                            label,
                            complemento.toString(),
                            response.totalAmount()
                    );
                }

                case QUERY_INSTALLMENT_COUNT -> {
                    TelegramInstallmentCountResponse response = financeBotApiClient.getInstallmentCount(
                            new TelegramInstallmentCountRequest(
                                    telegramId,
                                    parsedMessage.startDate(),
                                    parsedMessage.endDate()
                            )
                    );

                    yield telegramMessageFormatter.formatInstallmentCountMessage(
                            response.installmentCount(),
                            response.startDate(),
                            response.endDate()
                    );
                }

                case QUERY_INSTALLMENT_PURCHASE_CAPACITY -> {
                    InstallmentPurchaseCapacityResponse response =
                            financeBotApiClient.getInstallmentPurchaseCapacity(
                                    new InstallmentPurchaseCapacityRequest(
                                            telegramId,
                                            parsedMessage.totalAmount(),
                                            parsedMessage.totalInstallments()
                                    )
                            );

                    yield telegramMessageFormatter.formatInstallmentPurchaseCapacityMessage(
                            response.totalAmount(),
                            response.totalInstallments(),
                            response.estimatedInstallmentAmount(),
                            response.analysisResult(),
                            response.observation()
                    );
                }

                case QUERY_ACTIVE_INSTALLMENTS -> {
                    TelegramActiveInstallmentsResponse response =
                            financeBotApiClient.getActiveInstallments(telegramId);

                    yield telegramMessageFormatter.formatActiveInstallmentsMessage(
                            response.activeInstallmentGroupCount()
                    );
                }

                case QUERY_INSTALLMENT_REMAINING -> handleInstallmentRemaining(parsedMessage, telegramId);

                case QUERY_INSTALLMENT_END_DATE -> handleInstallmentEndDate(parsedMessage, telegramId);

                default -> "Não consegui interpretar sua consulta.";
            };

            saveQueryContextWithoutBlockingResponse(telegramId, parsedMessage);

            return resultMessage;
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            log.error(
                    "Erro ao processar consulta financeira. intentType={}, telegramId={}, categoryName={}, accountName={}, startDate={}, endDate={}",
                    parsedMessage != null ? parsedMessage.intentType() : null,
                    telegramId,
                    parsedMessage != null ? parsedMessage.categoryName() : null,
                    parsedMessage != null ? parsedMessage.accountName() : null,
                    parsedMessage != null ? parsedMessage.startDate() : null,
                    parsedMessage != null ? parsedMessage.endDate() : null,
                    e
            );

            return "Não foi possível consultar essas informações agora.";
        }
    }

    private void saveQueryContextWithoutBlockingResponse( Long telegramId, ParsedTelegramMessage parsedMessage) {
        try {
            telegramQueryContextService.saveQueryContext(telegramId, parsedMessage);
        } catch (Exception e) {
            log.warn(
                    "Não foi possível salvar contexto da consulta, mas a resposta será enviada normalmente. telegramId={}",
                    telegramId,
                    e
            );
        }
    }

    private String handleInstallmentRemaining(ParsedTelegramMessage parsedMessage, Long telegramId) {
        try {
            TelegramActiveInstallmentSummaryResponse response =
                    financeBotApiClient.getActiveInstallmentSummary(
                            telegramId,
                            parsedMessage.installmentQueryTarget()
                    );

            if (response == null || !response.hasActiveInstallment()) {
                if (parsedMessage.installmentQueryTarget() != null
                        && !parsedMessage.installmentQueryTarget().isBlank()) {
                    return telegramMessageFormatter.formatInstallmentNotFoundMessage(
                            parsedMessage.installmentQueryTarget()
                    );
                }

                return telegramMessageFormatter.formatNoActiveInstallmentsMessage();
            }

            return telegramMessageFormatter.formatRemainingInstallmentsMessage(
                    response.description(),
                    response.currentDueDate(),
                    response.currentInstallmentNumber(),
                    response.nextDueDate(),
                    response.remainingInstallments(),
                    response.nextInstallmentNumber(),
                    response.totalInstallments()
            );
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 403) {
                telegramPendingQueryService.savePending(telegramId, parsedMessage);
                return telegramMessageFormatter.formatMultipleActiveInstallmentsMessage();
            }

            throw e;
        }
    }

    private String handleInstallmentEndDate(ParsedTelegramMessage parsedMessage, Long telegramId) {
        try {
            TelegramActiveInstallmentSummaryResponse response =
                    financeBotApiClient.getActiveInstallmentSummary(
                            telegramId,
                            parsedMessage.installmentQueryTarget()
                    );

            if (response == null || !response.hasActiveInstallment()) {
                if (parsedMessage.installmentQueryTarget() != null
                        && !parsedMessage.installmentQueryTarget().isBlank()) {
                    return telegramMessageFormatter.formatInstallmentNotFoundMessage(
                            parsedMessage.installmentQueryTarget()
                    );
                }

                return telegramMessageFormatter.formatNoActiveInstallmentsMessage();
            }

            return telegramMessageFormatter.formatInstallmentEndDateMessage(
                    response.description(),
                    response.endDate()
            );
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 403) {
                telegramPendingQueryService.savePending(telegramId, parsedMessage);
                return telegramMessageFormatter.formatMultipleActiveInstallmentsMessage();
            }

            throw e;
        }
    }
}